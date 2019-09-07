package com.github.shingyx.boomswitch

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.util.Log
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val TAG = BoomClient::class.java.simpleName

private val SERVICE_UUID = UUID.fromString("000061fe-0000-1000-8000-00805f9b34fb")
private val WRITE_POWER_UUID = UUID.fromString("c6d6dc0d-07f5-47ef-9b59-630622b01fd3")
private val READ_STATE_UUID = UUID.fromString("4356a21c-a599-4b94-a1c8-4b91fca02a9a")

private const val BOOM_INACTIVE_STATE = 0.toByte()
private const val BOOM_POWER_ON = 1.toByte()
private const val BOOM_STANDBY = 2.toByte()
private const val TIMEOUT = 15000L

object BoomClient {
    private val lock = ReentrantLock()
    private var future: CompletionStage<Boolean>? = null

    fun switchPower(
        context: Context,
        reportProgress: (String) -> Unit
    ): CompletionStage<Boolean> {
        lock.withLock {
            var localFuture = future
            if (localFuture != null) {
                Log.i(TAG, "Already switching power, returning existing future")
            } else {
                localFuture = BoomClientInternal(context, reportProgress).switchPower()
                future = localFuture
                localFuture.whenComplete { _, _ ->
                    lock.withLock { future = null }
                }
            }
            return localFuture
        }
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
    private val handler = Handler()
    private val completableFuture = CompletableFuture<Boolean>()
    private var completeValue = false
    private lateinit var gatt: BluetoothGatt

    private var boomClientState = BoomClientState.NOT_STARTED
        set(value) {
            Log.i(TAG, "Setting boom client state to $value")
            when (value) {
                BoomClientState.CONNECTING -> "Connecting to speaker..."
                BoomClientState.CONNECTING_RETRY -> "First connection attempt failed, retrying..."
                BoomClientState.DISCOVERING_SERVICES -> "Switching speaker's power..."
                else -> null
            }?.also(reportProgress)
            field = value
        }

    fun switchPower(): CompletionStage<Boolean> {
        if (boomClientState == BoomClientState.NOT_STARTED) {
            initializeConnection()
            completableFuture
                .thenApply { "BOOM switched ${if (it) "on" else "off"}!" }
                .exceptionally { it.cause?.message ?: "Unknown error." }
                .thenApply(reportProgress)
            handler.postDelayed(this::onTimedOut, TIMEOUT)
        }
        return completableFuture
    }

    private fun resolve() {
        teardown()

        // Add a delay to complete the future at the same time the speaker plays a sound
        // and reduce the likelihood of issues on reconnect
        val delay = if (completeValue) 2500L else 1000L
        handler.postDelayed({
            Log.i(TAG, "Resolving future with $completeValue")
            completableFuture.complete(completeValue)
        }, delay)
    }

    private fun reject(exception: Exception) {
        Log.w(TAG, "Failed to switch power", exception)
        teardown()
        completableFuture.completeExceptionally(exception)
    }

    private fun teardown() {
        handler.removeCallbacksAndMessages(null)
        if (this::gatt.isInitialized) {
            gatt.close()
        }
        boomClientState = BoomClientState.COMPLETED
    }

    private fun onTimedOut() {
        val errorMessage =
            if (boomClientState == BoomClientState.CONNECTING || boomClientState == BoomClientState.CONNECTING_RETRY) {
                "Connection failed. Is the speaker in range?"
            } else {
                "Timed out switching speaker's power!"
            }
        reject(Exception(errorMessage))
    }

    private fun initializeConnection() {
        boomClientState = BoomClientState.CONNECTING
        val device = try {
            getDevice()
        } catch (e: Exception) {
            return reject(e)
        }
        val connectResult = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        if (connectResult != null) {
            gatt = connectResult
        } else {
            reject(Exception("Failed to create Bluetooth client. Is Bluetooth LE supported on your mobile device?"))
        }
    }

    private fun getDevice(): BluetoothDevice {
        val deviceInfo = Preferences.bluetoothDeviceInfo
            ?: throw Exception("Please select your speaker in the BOOM Switch app.")

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()?.takeIf { it.isEnabled }
            ?: throw Exception("Bluetooth is turned off. Please turn on Bluetooth then try again.")

        return bluetoothAdapter.bondedDevices.find { it.address == deviceInfo.address }
            ?: throw Exception("Failed to find \"${deviceInfo.name}\"'s address. Has the speaker been unpaired?")
    }

    override fun onConnectionStateChange(status: Int, newState: Int) {
        if (newState != BluetoothProfile.STATE_CONNECTED) {
            when (boomClientState) {
                BoomClientState.CONNECTING -> {
                    boomClientState = BoomClientState.CONNECTING_RETRY
                    Log.i(TAG, "Connection attempt failed, retrying")
                    if (!gatt.connect()) {
                        reject(Exception("Failed to retry connection."))
                    }
                }
                BoomClientState.DISCONNECTING -> resolve()
                else -> reject(Exception("Unexpectedly disconnected from speaker."))
            }
            return
        }

        boomClientState = BoomClientState.DISCOVERING_SERVICES
        if (!gatt.discoverServices()) {
            reject(Exception("Failed to start discovering Bluetooth LE services."))
        }
    }

    override fun onServicesDiscovered(status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return reject(Exception("Failed to discover Bluetooth LE services."))
        }

        boomClientState = BoomClientState.READING_STATE
        val stateCharacteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(READ_STATE_UUID)
        if (stateCharacteristic == null || !gatt.readCharacteristic(stateCharacteristic)) {
            reject(Exception("Failed to start reading the speaker's current state. The selected speaker might not be supported."))
        }
    }

    override fun onCharacteristicRead(characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return reject(Exception("Failed to read the speaker's current state."))
        }

        boomClientState = BoomClientState.WRITING_POWER
        val powerCharacteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(WRITE_POWER_UUID)
        if (powerCharacteristic != null) {
            val message = ByteArray(7)
            completeValue = characteristic.value[0] == BOOM_INACTIVE_STATE
            message[6] = if (completeValue) {
                BOOM_POWER_ON
            } else {
                BOOM_STANDBY
            }
            powerCharacteristic.value = message
            if (gatt.writeCharacteristic(powerCharacteristic)) {
                return
            }
        }
        reject(Exception("Failed to start setting the speaker's new state. The selected speaker might not be supported."))
    }

    override fun onCharacteristicWrite(characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return reject(Exception("Failed to set the speaker's new state."))
        }

        boomClientState = BoomClientState.DISCONNECTING
        gatt.disconnect()
    }
}

private abstract class GattCallbackWrapper {
    protected val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.v(TAG, "onConnectionStateChange: $status, $newState")
            onConnectionStateChange(status, newState)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.v(TAG, "onServicesDiscovered: $status")
            onServicesDiscovered(status)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int) {
            Log.v(TAG, "onCharacteristicRead: $status, ${char.uuid} = [${char.value?.joinToString()}]")
            onCharacteristicRead(char, status)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int) {
            Log.v(TAG, "onCharacteristicWrite: $status, ${char.uuid} = [${char.value?.joinToString()}]")
            onCharacteristicWrite(char, status)
        }
    }

    protected abstract fun onConnectionStateChange(status: Int, newState: Int)
    protected abstract fun onServicesDiscovered(status: Int)
    protected abstract fun onCharacteristicRead(characteristic: BluetoothGattCharacteristic, status: Int)
    protected abstract fun onCharacteristicWrite(characteristic: BluetoothGattCharacteristic, status: Int)
}
