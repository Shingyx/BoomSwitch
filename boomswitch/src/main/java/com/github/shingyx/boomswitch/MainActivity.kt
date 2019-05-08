package com.github.shingyx.boomswitch

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

private val TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {
    private lateinit var toaster: Toaster

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toaster = Toaster(this) { runOnUiThread(it) }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // TODO verify BT is supported and enabled

        button.setOnClickListener {
            val pairedDevices = bluetoothAdapter.bondedDevices

            // TODO make this selectable
            val boomDevice = pairedDevices.first()

            Log.d(TAG, "${boomDevice.name}: ${boomDevice.address}")

            val deviceInfo = BluetoothDeviceInfo(boomDevice)
            BoomClient.switchPower(this, deviceInfo) {
                runOnUiThread { toaster.show(it) }
            }
        }
    }
}
