package com.github.shingyx.boomswitch.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.boomswitch.data.BluetoothDeviceInfo
import com.github.shingyx.boomswitch.data.BoomClient
import com.github.shingyx.boomswitch.data.Preferences
import com.github.shingyx.boomswitch.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var handler: Handler
    private lateinit var adapter: BluetoothDeviceAdapter
    private lateinit var bluetoothStateReceiver: BroadcastReceiver

    private val bluetoothOffAlertDialog by lazy {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.bluetooth_turned_off_alert)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Preferences.initialize(this)
        handler = Handler()
        adapter = BluetoothDeviceAdapter(this)
        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    updateBluetoothDevices()
                }
            }
        }

        select_speaker.setAdapter(adapter)
        select_speaker.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Preferences.bluetoothDeviceInfo = adapter.getItem(position)
        }
        select_speaker.setText(Preferences.bluetoothDeviceInfo?.toString())

        switch_button.setOnClickListener { switchPower() }

        registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    override fun onResume() {
        super.onResume()
        updateBluetoothDevices()
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothStateReceiver)
        super.onDestroy()
    }

    private fun switchPower() {
        handler.removeCallbacksAndMessages(null)

        switch_button.isEnabled = false
        fadeView(progress_bar, true)
        progress_description.text = ""
        fadeView(progress_description, true)

        BoomClient.switchPower(this, this::reportProgress)
            .whenComplete { _, _ ->
                runOnUiThread {
                    switch_button.isEnabled = true
                    fadeView(progress_bar, false)
                }
                handler.postDelayed({
                    fadeView(progress_description, false)
                }, 4000)
            }
    }

    private fun fadeView(view: View, show: Boolean) {
        if (show) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
        }
        view.animate()
            .alpha(if (show) 1f else 0f)
            .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    if (!show) {
                        view.visibility = View.GONE
                    }
                }
            })
    }

    private fun reportProgress(message: String) {
        runOnUiThread {
            progress_description.text = message
        }
    }

    private fun updateBluetoothDevices() {
        val bondedDevices = BluetoothAdapter.getDefaultAdapter()
            ?.takeIf { it.isEnabled }
            ?.bondedDevices

        val devicesInfo = if (bondedDevices != null) {
            bluetoothOffAlertDialog.hide()
            bondedDevices.map { BluetoothDeviceInfo(it) }.sorted()
        } else {
            bluetoothOffAlertDialog.show()
            emptyList()
        }

        select_speaker_container.error = if (devicesInfo.isEmpty()) {
            getString(R.string.no_devices_found)
        } else {
            null
        }

        adapter.updateItems(devicesInfo)
    }
}
