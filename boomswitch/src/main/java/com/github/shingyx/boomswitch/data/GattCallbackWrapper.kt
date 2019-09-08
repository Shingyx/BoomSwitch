package com.github.shingyx.boomswitch.data

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log

private val TAG = GattCallbackWrapper::class.java.simpleName

abstract class GattCallbackWrapper {
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
