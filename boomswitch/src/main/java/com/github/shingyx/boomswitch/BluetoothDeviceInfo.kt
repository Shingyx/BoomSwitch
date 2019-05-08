package com.github.shingyx.boomswitch

import android.bluetooth.BluetoothDevice

data class BluetoothDeviceInfo(val name: String, val address: String) {
    constructor(device: BluetoothDevice) : this(device.name, device.address)
}
