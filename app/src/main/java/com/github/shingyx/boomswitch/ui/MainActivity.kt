package com.github.shingyx.boomswitch.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.boomswitch.R
import com.github.shingyx.boomswitch.data.BluetoothDeviceInfo
import com.github.shingyx.boomswitch.data.BoomClient
import com.github.shingyx.boomswitch.data.Preferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var handler: Handler
    private lateinit var adapter: BluetoothDeviceAdapter
    private lateinit var bluetoothStateReceiver: BluetoothStateReceiver

    private val bluetoothOffAlertDialog = lazy {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.bluetooth_turned_off_alert)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handler = Handler()
        adapter = BluetoothDeviceAdapter(this)
        bluetoothStateReceiver = BluetoothStateReceiver(this::updateBluetoothDevices)

        select_speaker.setAdapter(adapter)
        select_speaker.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Preferences.bluetoothDeviceInfo = adapter.getItem(position)
        }
        select_speaker.setText(Preferences.bluetoothDeviceInfo?.toString())

        switch_button.setOnClickListener { launch { switchBoom() } }

        registerReceiver(bluetoothStateReceiver, BluetoothStateReceiver.intentFilter())
    }

    override fun onResume() {
        super.onResume()
        updateBluetoothDevices()
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothStateReceiver)
        super.onDestroy()
    }

    private suspend fun switchBoom() {
        handler.removeCallbacksAndMessages(null)

        switch_button.isEnabled = false
        fadeView(progress_bar, true)
        progress_description.text = ""
        fadeView(progress_description, true)

        BoomClient.switchPower(this) { progressMessage ->
            runOnUiThread {
                progress_description.text = progressMessage
            }
        }

        switch_button.isEnabled = true
        fadeView(progress_bar, false)
        handler.postDelayed({
            fadeView(progress_description, false)
        }, 4000)
    }

    private fun fadeView(view: View, show: Boolean) {
        val newAlpha = if (show) 1f else 0f
        view.visibility = View.VISIBLE
        view.alpha = 1f - newAlpha
        view.animate()
            .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
            .alpha(newAlpha)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    view.visibility = if (show) View.VISIBLE else View.GONE
                }
            })
    }

    private fun updateBluetoothDevices() {
        val bondedDevices = BluetoothAdapter.getDefaultAdapter()
            ?.takeIf { it.isEnabled }
            ?.bondedDevices

        val devicesInfo = if (bondedDevices != null) {
            if (bluetoothOffAlertDialog.isInitialized()) {
                bluetoothOffAlertDialog.value.hide()
            }
            bondedDevices.map { BluetoothDeviceInfo(it) }.sorted()
        } else {
            bluetoothOffAlertDialog.value.show()
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
