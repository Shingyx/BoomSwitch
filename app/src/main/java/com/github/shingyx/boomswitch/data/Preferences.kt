package com.github.shingyx.boomswitch.data

import android.content.Context
import android.content.SharedPreferences

object Preferences {
    private const val SHARED_PREFERENCES_NAME = "BoomSwitchData"
    private const val KEY_DEVICE_NAME = "DeviceName"
    private const val KEY_DEVICE_ADDRESS = "DeviceAddress"

    private lateinit var sharedPreferences: SharedPreferences

    var bluetoothDeviceInfo: BluetoothDeviceInfo?
        get() {
            val name = sharedPreferences.getString(KEY_DEVICE_NAME, null)
            val address = sharedPreferences.getString(KEY_DEVICE_ADDRESS, null)
            return if (name != null && address != null) BluetoothDeviceInfo(name, address) else null
        }
        set(value) {
            if (value != bluetoothDeviceInfo) {
                sharedPreferences.edit()
                    .putString(KEY_DEVICE_NAME, value?.name)
                    .putString(KEY_DEVICE_ADDRESS, value?.address)
                    .apply()
            }
        }

    fun initialize(context: Context) {
        if (!this::sharedPreferences.isInitialized) {
            sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        }
    }
}
