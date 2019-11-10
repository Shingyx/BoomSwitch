package com.github.shingyx.boomswitch.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.annotation.StringRes
import com.github.shingyx.boomswitch.R
import kotlinx.coroutines.CompletableDeferred
import java.util.*

private val SERVICE_UUID = UUID.fromString("000061fe-0000-1000-8000-00805f9b34fb")
private val WRITE_POWER_UUID = UUID.fromString("c6d6dc0d-07f5-47ef-9b59-630622b01fd3")
private val READ_STATE_UUID = UUID.fromString("4356a21c-a599-4b94-a1c8-4b91fca02a9a")

private const val BOOM_INACTIVE_STATE = 0.toByte()
private const val BOOM_POWER_ON = 1.toByte()
private const val BOOM_STANDBY = 2.toByte()
private const val TIMEOUT = 15000L

object BoomClient {
    private val tag = javaClass.simpleName

    @Volatile
    private var inProgress = false

    suspend fun switchPower(
        context: Context,
        reportProgress: (String) -> Unit
    ) {
        if (inProgress) {
            Log.w(tag, "Switching already in progress")
            reportProgress(context.getString(R.string.error_switching_already_in_progress))
            return
        }

        inProgress = true
        BoomClientInternal(context, reportProgress).switchPower()
        inProgress = false
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

private class BoomClientInternal(
    private val context: Context,
    private val reportProgress: (String) -> Unit
) : GattCallbackWrapper() {
    override val tag = javaClass.simpleName

    private val handler = Handler()
    private val deferred = CompletableDeferred<Unit>()
    private var switchingOn = false
    private lateinit var gatt: BluetoothGatt

    private var boomClientState = BoomClientState.NOT_STARTED
        set(value) {
            Log.i(tag, "Setting boom client state to $value")
            when (value) {
                BoomClientState.CONNECTING -> R.string.connecting_to_speaker
                BoomClientState.CONNECTING_RETRY -> R.string.retry_connecting_to_speaker
                BoomClientState.DISCOVERING_SERVICES -> R.string.switching_speakers_power
                else -> null
            }?.let { reportProgress(context.getString(it)) }
            field = value
        }

    suspend fun switchPower() {
        if (boomClientState == BoomClientState.NOT_STARTED) {
            handler.postDelayed(this::onTimedOut, TIMEOUT)
            initializeConnection()
        }
        return deferred.await()
    }

    private fun resolve() {
        teardown()

        // Add a delay to complete the deferred at the same time the speaker plays a sound
        // and reduce the likelihood of issues on reconnect
        val delay = if (switchingOn) 2500L else 1000L
        handler.postDelayed({
            Log.i(tag, "Resolving deferred with $switchingOn")
            val resId = if (switchingOn) R.string.boom_switched_on else R.string.boom_switched_off
            reportProgress(context.getString(resId))
            deferred.complete(Unit)
        }, delay)
    }

    private fun reject(message: String, @StringRes resId: Int, vararg formatArgs: String) {
        val exception = Exception(message)
        Log.w(tag, "Failed to switch power", exception)
        teardown()
        reportProgress(context.getString(resId, formatArgs))
        deferred.complete(Unit)
    }

    private fun teardown() {
        handler.removeCallbacksAndMessages(null)
        if (this::gatt.isInitialized) {
            gatt.close()
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

    private fun initializeConnection() {
        boomClientState = BoomClientState.CONNECTING

        val deviceInfo = Preferences.bluetoothDeviceInfo
            ?: return reject("No speaker selected", R.string.error_no_speaker_selected)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()?.takeIf { it.isEnabled }
            ?: return reject("Bluetooth disabled", R.string.error_bluetooth_disabled)

        val device = bluetoothAdapter.bondedDevices.find { it.address == deviceInfo.address }
            ?: return reject("Speaker not paired", R.string.error_speaker_unpaired, deviceInfo.name)

        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            ?: return reject("connectGatt returned null", R.string.error_null_bluetooth_client)
    }

    override fun onConnectionStateChange(status: Int, newState: Int) {
        if (newState != BluetoothProfile.STATE_CONNECTED) {
            when (boomClientState) {
                BoomClientState.CONNECTING -> {
                    boomClientState = BoomClientState.CONNECTING_RETRY
                    Log.i(tag, "Connection attempt failed, retrying")
                    if (!gatt.connect()) {
                        reject("Retry connection failed", R.string.error_connection_failed)
                    }
                }
                BoomClientState.DISCONNECTING -> resolve()
                else -> reject("Unexpected disconnect", R.string.error_unexpected_disconnect)
            }
            return
        }

        boomClientState = BoomClientState.DISCOVERING_SERVICES
        if (!gatt.discoverServices()) {
            reject("discoverServices failed", R.string.error_switching_power_failed)
        }
    }

    override fun onServicesDiscovered(status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            reject("onServicesDiscovered status is $status", R.string.error_switching_power_failed)
            return
        }

        boomClientState = BoomClientState.READING_STATE
        val stateCharacteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(READ_STATE_UUID)
        if (stateCharacteristic == null || !gatt.readCharacteristic(stateCharacteristic)) {
            reject("readCharacteristic failed", R.string.error_switching_power_failed)
        }
    }

    override fun onCharacteristicRead(characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            reject("onCharacteristicRead status is $status", R.string.error_switching_power_failed)
            return
        }

        boomClientState = BoomClientState.WRITING_POWER
        val powerCharacteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(WRITE_POWER_UUID)
        if (powerCharacteristic != null) {
            val message = ByteArray(7)
            switchingOn = characteristic.value[0] == BOOM_INACTIVE_STATE
            message[6] = if (switchingOn) {
                BOOM_POWER_ON
            } else {
                BOOM_STANDBY
            }
            powerCharacteristic.value = message
            if (gatt.writeCharacteristic(powerCharacteristic)) {
                return
            }
        }

        reject("writeCharacteristic failed", R.string.error_switching_power_failed)
    }

    override fun onCharacteristicWrite(characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            reject("onCharacteristicWrite status is $status", R.string.error_switching_power_failed)
            return
        }

        boomClientState = BoomClientState.DISCONNECTING
        gatt.disconnect()
    }
}