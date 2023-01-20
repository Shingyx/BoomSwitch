package com.github.shingyx.boomswitch.data

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import timber.log.Timber

abstract class GattCallbackWrapper {
    protected val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Timber.v("onConnectionStateChange: $status, $newState")
            onConnectionStateChange(status, newState)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Timber.v("onServicesDiscovered: $status")
            onServicesDiscovered(status)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            Timber.v("onCharacteristicRead: $status, ${char.uuid} = [${char.value?.joinToString()}]")
            onCharacteristicRead(char, status)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int) {
            Timber.v("onCharacteristicWrite: $status, ${char.uuid} = [${char.value?.joinToString()}]")
            onCharacteristicWrite(char, status)
        }
    }

    protected abstract fun onConnectionStateChange(status: Int, newState: Int)
    protected abstract fun onServicesDiscovered(status: Int)
    protected abstract fun onCharacteristicRead(characteristic: BluetoothGattCharacteristic, status: Int)
    protected abstract fun onCharacteristicWrite(characteristic: BluetoothGattCharacteristic, status: Int)
}
