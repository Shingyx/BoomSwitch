package com.github.shingyx.boomswitch.ui

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BluetoothStateReceiver(private val onStateChanged: () -> Unit) : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
      val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
      if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_ON) {
        onStateChanged()
      }
    }
  }

  companion object {
    fun intentFilter(): IntentFilter {
      return IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
    }
  }
}
