package com.github.shingyx.boomswitch.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import com.github.shingyx.boomswitch.R
import kotlinx.coroutines.CompletableDeferred
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val SERVICE_UUID = UUID.fromString("000061fe-0000-1000-8000-00805f9b34fb")
private val WRITE_POWER_UUID = UUID.fromString("c6d6dc0d-07f5-47ef-9b59-630622b01fd3")
private val READ_STATE_UUID = UUID.fromString("4356a21c-a599-4b94-a1c8-4b91fca02a9a")

// READ_STATE_UUID values
private const val BOOM_INACTIVE_STATE = 0.toByte()

// WRITE_POWER_UUID values
private const val BOOM_DISABLE_REMOTE_POWER = 0.toByte()

private const val TIMEOUT = 15000L

object BoomClient {
    private val inProgressMap = ConcurrentHashMap<String, Unit>()

    suspend fun disableRemotePower(
        context: Context,
        deviceInfo: BluetoothDeviceInfo,
        reportProgress: (String) -> Unit
    ) {
        if (inProgressMap.putIfAbsent(deviceInfo.address, Unit) != null) {
            Timber.w("Disabling already in progress")
            return reportProgress(
                context.getString(R.string.error_disabling_already_in_progress, deviceInfo.name)
            )
        }

        BoomClientInternal(context, deviceInfo, reportProgress).disableRemotePower()
        inProgressMap.remove(deviceInfo.address)
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevicesInfo(): List<BluetoothDeviceInfo>? {
        try {
            val bondedDevices = BluetoothAdapter.getDefaultAdapter()
                ?.takeIf { it.isEnabled }
                ?.bondedDevices

            if (bondedDevices != null) {
                return bondedDevices.map { BluetoothDeviceInfo(it.name, it.address) }.sorted()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read bonded devices")
        }
        return null
    }

    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
    }
}

private enum class BoomClientState {
    NOT_STARTED,
    CONNECTING,
    CONNECTING_RETRY,
    DISCOVERING_SERVICES,
    READING_STATE,
    WRITING_POWER,
    DISCONNECTING,
    COMPLETED,
}

@SuppressLint("MissingPermission")
private class BoomClientInternal(
    private val context: Context,
    private val deviceInfo: BluetoothDeviceInfo,
    private val reportProgress: (String) -> Unit
) : GattCallbackWrapper() {
    private val handler = Handler(Looper.getMainLooper())
    private val deferred = CompletableDeferred<Unit>()

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var device: BluetoothDevice
    private lateinit var gatt: BluetoothGatt
    private var bluetoothA2dp: BluetoothA2dp? = null

    private var boomClientState = BoomClientState.NOT_STARTED
        set(value) {
            Timber.i("Setting boom client state to $value")
            when (value) {
                BoomClientState.CONNECTING -> R.string.connecting_to_speaker
                BoomClientState.CONNECTING_RETRY -> R.string.retry_connecting_to_speaker
                BoomClientState.DISCOVERING_SERVICES -> R.string.disabling_speakers_power
                else -> null
            }?.let { reportProgress(context.getString(it, deviceInfo.name)) }
            field = value
        }

    suspend fun disableRemotePower() {
        if (boomClientState == BoomClientState.NOT_STARTED) {
            handler.postDelayed(this::onTimedOut, TIMEOUT)
            initializeConnection()
        }
        return deferred.await()
    }

    private fun resolve() {
        teardown()

        val delay = 1000L
        handler.postDelayed({
            Timber.i("Resolving deferred")
            reportProgress(context.getString(R.string.boom_remote_power_disabled, deviceInfo.name))
            deferred.complete(Unit)
        }, delay)
    }

    private fun reject(message: String, @StringRes resId: Int) {
        Timber.w(Exception(message), "Failed to disable")
        teardown()
        reportProgress(context.getString(resId, deviceInfo.name))
        deferred.complete(Unit)
    }

    private fun teardown() {
        handler.removeCallbacksAndMessages(null)
        if (this::gatt.isInitialized) {
            gatt.close()
        }
        if (bluetoothA2dp != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp)
        }
        boomClientState = BoomClientState.COMPLETED
    }

    private fun onTimedOut() {
        val resId =
            if (boomClientState == BoomClientState.CONNECTING || boomClientState == BoomClientState.CONNECTING_RETRY) {
                R.string.error_connection_failed
            } else {
                R.string.error_timed_out
            }
        reject("Timed out in state $boomClientState", resId)
    }

    /**
     * Initiates connecting to the speaker's Gatt server.
     * Called by entry function [disableRemotePower] and continued by [onConnectionStateChange].
     */
    private fun initializeConnection() {
        boomClientState = BoomClientState.CONNECTING

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()?.takeIf { it.isEnabled }
            ?: return reject("Bluetooth disabled", R.string.error_bluetooth_disabled)

        device = bluetoothAdapter.bondedDevices.find { it.address == deviceInfo.address }
            ?: return reject("Speaker not paired", R.string.error_speaker_unpaired)

        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            ?: return reject("connectGatt returned null", R.string.error_null_bluetooth_client)

        bluetoothAdapter.getProfileProxy(
            context.applicationContext,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    Timber.v("getProfileProxy onServiceConnected")
                    // Note: small risk of Gatt process completing before bluetoothA2dp is assigned
                    bluetoothA2dp = proxy as BluetoothA2dp
                    if (boomClientState == BoomClientState.COMPLETED) {
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Timber.v("getProfileProxy onServiceDisconnected")
                }
            },
            BluetoothProfile.A2DP
        )
    }

    /**
     * Scans for the available Gatt services and characteristics.
     * Called by [onConnectionStateChange] and continued by [onServicesDiscovered].
     */
    private fun discoverGattServices() {
        boomClientState = BoomClientState.DISCOVERING_SERVICES

        if (!gatt.discoverServices()) {
            reject("discoverServices failed", R.string.error_disabling_power_failed)
        }
    }

    /**
     * Reads the speaker's current state.
     * Called by [onServicesDiscovered] and continued by [onCharacteristicRead].
     */
    private fun readStateCharacteristic() {
        boomClientState = BoomClientState.READING_STATE

        val stateCharacteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(READ_STATE_UUID)
        if (stateCharacteristic == null || !gatt.readCharacteristic(stateCharacteristic)) {
            reject("readCharacteristic failed", R.string.error_disabling_power_failed)
        }
    }

    /**
     * Writes the new power state for the speaker to toggle it on or off.
     * Called by [onCharacteristicRead] and continued by [onCharacteristicWrite].
     */
    private fun writePowerCharacteristic(speakerIsInactive: Boolean) {
        boomClientState = BoomClientState.WRITING_POWER

        val powerByteValue = BOOM_DISABLE_REMOTE_POWER

        val powerCharacteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(WRITE_POWER_UUID)
        if (powerCharacteristic != null) {
            powerCharacteristic.value = byteArrayOf(0, 0, 0, 0, 0, 0, powerByteValue)
            if (gatt.writeCharacteristic(powerCharacteristic)) {
                return
            }
        }

        reject("writeCharacteristic failed", R.string.error_disabling_power_failed)
    }

    /**
     * Closes the Gatt connection with the speaker.
     * Called by [onCharacteristicWrite] and continued by [onConnectionStateChange].
     */
    private fun disconnectSpeaker() {
        boomClientState = BoomClientState.DISCONNECTING
        gatt.disconnect()
    }

    override fun onConnectionStateChange(status: Int, newState: Int) {
        if (newState != BluetoothProfile.STATE_CONNECTED) {
            when (boomClientState) {
                BoomClientState.CONNECTING -> {
                    boomClientState = BoomClientState.CONNECTING_RETRY
                    Timber.i("Connection attempt failed, retrying")
                    if (!gatt.connect()) {
                        reject("Retry connection failed", R.string.error_connection_failed)
                    }
                }
                BoomClientState.DISCONNECTING -> resolve()
                else -> reject("Unexpected disconnect", R.string.error_unexpected_disconnect)
            }
            return
        }

        discoverGattServices()
    }

    override fun onServicesDiscovered(status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            reject("onServicesDiscovered status is $status", R.string.error_disabling_power_failed)
            return
        }

        readStateCharacteristic()
    }

    override fun onCharacteristicRead(characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            reject("onCharacteristicRead status is $status", R.string.error_disabling_power_failed)
            return
        }

        val speakerIsInactive = characteristic.value[0] == BOOM_INACTIVE_STATE
        writePowerCharacteristic(speakerIsInactive)
    }

    override fun onCharacteristicWrite(characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            if (status == 133) {
                // 133 error seems to be a successful disable
                Timber.i("Ignoring onCharacteristicWrite 133 status")
            } else {
                reject("onCharacteristicWrite status is $status", R.string.error_disabling_power_failed)
                return
            }
        }

        disconnectSpeaker()
    }
}
