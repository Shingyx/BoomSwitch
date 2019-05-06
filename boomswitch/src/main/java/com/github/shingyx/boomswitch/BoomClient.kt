package com.github.shingyx.boomswitch

import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

private enum class BoomClientState {
    STOPPED,
    CONNECTING,
    CONNECTING_RETRY,
    DISCOVERING_SERVICES,
    READING_STATE,
    WRITING_POWER,
}

private const val TAG = "BoomClient"

private val SERVICE_UUID = UUID.fromString("000061fe-0000-1000-8000-00805f9b34fb")
private val WRITE_POWER_UUID = UUID.fromString("c6d6dc0d-07f5-47ef-9b59-630622b01fd3")
private val READ_STATE_UUID = UUID.fromString("4356a21c-a599-4b94-a1c8-4b91fca02a9a")

private const val BOOM_INACTIVE_STATE = 0.toByte()
private const val BOOM_POWER_ON = 1.toByte()
private const val BOOM_STANDBY = 2.toByte()

private var boomClientState = BoomClientState.STOPPED
private var future = CompletableFuture<Boolean?>()

fun switchPower(context: Context, device: BluetoothDevice): CompletionStage<Boolean?> {
    if (boomClientState != BoomClientState.STOPPED) {
        Log.i(TAG, "Already switching power, returning existing future")
    } else {
        future = CompletableFuture()
        connectToDevice(context, device)
    }
    return future
}

private val gattCallback = object : BluetoothGattCallback() {
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        Log.v(TAG, "onConnectionStateChange: $status, $newState")
        if (newState != BluetoothProfile.STATE_CONNECTED) {
            if (boomClientState == BoomClientState.CONNECTING) {
                Log.i(TAG, "Connection attempt failed, retrying")
                retryConnectToDevice(gatt)
            } else {
                if (boomClientState == BoomClientState.CONNECTING_RETRY) {
                    Log.e(TAG, "Connection retry failed")
                } else {
                    Log.e(TAG, "Unexpectedly disconnected from device")
                }
                dispose(gatt)
            }
            return
        }
        discoverServices(gatt)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Log.v(TAG, "onServicesDiscovered $status")
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "Failed to discover services")
            return dispose(gatt)
        }
        readStateCharacteristic(gatt)
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        Log.v(TAG, "onCharacteristicRead: $status, ${characteristic.uuid} = [${characteristic.value?.joinToString()}]")
        if (status != BluetoothGatt.GATT_SUCCESS || characteristic.uuid != READ_STATE_UUID) {
            Log.e(TAG, "Failed to read state characteristic")
            return dispose(gatt)
        }
        writePowerCharacteristic(gatt, characteristic.value[0])
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        Log.v(TAG, "onCharacteristicWrite: $status, ${characteristic.uuid} = [${characteristic.value?.joinToString()}]")
        val completeValue = if (status != BluetoothGatt.GATT_SUCCESS || characteristic.uuid != WRITE_POWER_UUID) {
            Log.e(TAG, "Failed to write power characteristic")
            null
        } else {
            characteristic.value[6] == BOOM_POWER_ON
        }
        dispose(gatt, completeValue)
    }
}

private fun connectToDevice(context: Context, device: BluetoothDevice) {
    if (device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE) != null) {
        boomClientState = BoomClientState.CONNECTING
        return
    }
    Log.e(TAG, "Failed to create GATT client")
    dispose(null)
}

private fun retryConnectToDevice(gatt: BluetoothGatt) {
    if (gatt.connect()) {
        boomClientState = BoomClientState.CONNECTING_RETRY
        return
    }
    Log.e(TAG, "Failed to retry connection")
    dispose(gatt)
}

private fun discoverServices(gatt: BluetoothGatt) {
    if (gatt.discoverServices()) {
        boomClientState = BoomClientState.DISCOVERING_SERVICES
        return
    }
    Log.e(TAG, "Failed to start discovering services")
    dispose(gatt)
}

private fun readStateCharacteristic(gatt: BluetoothGatt) {
    val stateCharacteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(READ_STATE_UUID)
    if (stateCharacteristic != null && gatt.readCharacteristic(stateCharacteristic)) {
        boomClientState = BoomClientState.READING_STATE
        return
    }
    Log.e(TAG, "Failed to start reading state characteristic")
    dispose(gatt)
}

private fun writePowerCharacteristic(gatt: BluetoothGatt, deviceState: Byte) {
    val powerCharacteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(WRITE_POWER_UUID)
    if (powerCharacteristic != null) {
        val message = ByteArray(7)
        message[6] = if (deviceState == BOOM_INACTIVE_STATE) {
            BOOM_POWER_ON
        } else {
            BOOM_STANDBY
        }
        powerCharacteristic.value = message
        if (gatt.writeCharacteristic(powerCharacteristic)) {
            boomClientState = BoomClientState.WRITING_POWER
            return
        }
    }
    Log.e(TAG, "Failed to start writing power characteristic")
    dispose(gatt)
}

private fun dispose(gatt: BluetoothGatt?, completeValue: Boolean? = null) {
    gatt?.close()
    future.complete(completeValue)
    boomClientState = BoomClientState.STOPPED
}
