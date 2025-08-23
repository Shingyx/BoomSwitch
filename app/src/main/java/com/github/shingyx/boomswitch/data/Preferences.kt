package com.github.shingyx.boomswitch.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

object Preferences {
  private const val SHARED_PREFERENCES_NAME = "BoomSwitchData"
  private const val KEY_DEVICE_NAME = "DeviceName"
  private const val KEY_DEVICE_ADDRESS = "DeviceAddress"
  private const val KEY_NIGHT_MODE = "NightMode"

  private lateinit var sharedPreferences: SharedPreferences

  var bluetoothDeviceInfo: BluetoothDeviceInfo?
    get() {
      val name = sharedPreferences.getString(KEY_DEVICE_NAME, null) ?: return null
      val address = sharedPreferences.getString(KEY_DEVICE_ADDRESS, null) ?: return null
      return BluetoothDeviceInfo(name, address)
    }
    set(value) {
      sharedPreferences.edit {
        putString(KEY_DEVICE_NAME, value?.name)
        putString(KEY_DEVICE_ADDRESS, value?.address)
      }
    }

  var appColorTheme: AppColorTheme
    get() {
      val nightModeValue =
          sharedPreferences.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
      return AppColorTheme.fromNightModeValue(nightModeValue)
    }
    set(value) {
      sharedPreferences.edit { putInt(KEY_NIGHT_MODE, value.nightModeValue) }
    }

  fun initialize(context: Context) {
    if (!this::sharedPreferences.isInitialized) {
      sharedPreferences =
          context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
  }
}
