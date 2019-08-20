package com.github.shingyx.boomswitch

import android.bluetooth.BluetoothDevice

data class BluetoothDeviceInfo(
    val name: String,
    val address: String
): Comparable<BluetoothDeviceInfo> {
    constructor(device: BluetoothDevice) : this(device.name, device.address)

    override fun compareTo(other: BluetoothDeviceInfo): Int {
        return name.compareTo(other.name, true)
    }
}
