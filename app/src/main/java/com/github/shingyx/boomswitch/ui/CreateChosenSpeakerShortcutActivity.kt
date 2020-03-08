package com.github.shingyx.boomswitch.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.boomswitch.R
import com.github.shingyx.boomswitch.data.BluetoothDeviceInfo
import kotlinx.android.synthetic.main.activity_create_chosen_speaker_shortcut.*
import timber.log.Timber

class CreateChosenSpeakerShortcutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chosen_speaker_shortcut)
        setSupportActionBar(toolbar)

        if (intent.action != Intent.ACTION_CREATE_SHORTCUT) {
            Timber.w("Unexpected intent action ${intent.action}")
            return finish()
        }

        val adapter = createBluetoothDeviceAdapter()
            ?: return finish()

        speaker_list.adapter = adapter
        speaker_list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedSpeaker = adapter.getItem(position)
            val intent = ShortcutActivity.createShortcutIntent(this, selectedSpeaker)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun createBluetoothDeviceAdapter(): BluetoothDeviceAdapter? {
        var devicesInfo: List<BluetoothDeviceInfo>? = null

        try {
            val bondedDevices = BluetoothAdapter.getDefaultAdapter()
                ?.takeIf { it.isEnabled }
                ?.bondedDevices

            if (bondedDevices != null) {
                devicesInfo = bondedDevices.map { BluetoothDeviceInfo(it) }.sorted()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read bonded devices")
        }

        if (devicesInfo == null) {
            Toast.makeText(this, R.string.error_bluetooth_disabled, Toast.LENGTH_LONG).show()
            return null
        }

        if (devicesInfo.isEmpty()) {
            Toast.makeText(this, R.string.no_devices_found, Toast.LENGTH_LONG).show()
            return null
        }

        return BluetoothDeviceAdapter(this, devicesInfo)
    }
}
