package com.github.shingyx.boomswitch.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.boomswitch.R
import com.github.shingyx.boomswitch.data.BluetoothDeviceInfo
import com.github.shingyx.boomswitch.data.BoomClient
import com.github.shingyx.boomswitch.databinding.ActivityCreateShortcutBinding
import timber.log.Timber

class CreateShortcutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateShortcutBinding

    private val fakeUseLastSelectedSpeakerDevice =
        BluetoothDeviceInfo("Use last selected speaker", "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateShortcutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (intent.action != Intent.ACTION_CREATE_SHORTCUT) {
            Timber.w("Unexpected intent action ${intent.action}")
            return finish()
        }

        val adapter = createBluetoothDeviceAdapter()
            ?: return finish()

        binding.speakerList.adapter = adapter
        binding.speakerList.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val selectedSpeaker = adapter.getItem(pos).takeUnless {
                it == fakeUseLastSelectedSpeakerDevice
            }
            val intent = ShortcutActivity.createShortcutIntent(this, selectedSpeaker)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun createBluetoothDeviceAdapter(): BluetoothDeviceAdapter? {
        val devicesInfo = BoomClient.getPairedDevicesInfo()

        if (devicesInfo == null) {
            Toast.makeText(this, R.string.error_bluetooth_disabled, Toast.LENGTH_LONG).show()
            return null
        }

        if (devicesInfo.isEmpty()) {
            Toast.makeText(this, R.string.no_devices_found, Toast.LENGTH_LONG).show()
            return null
        }

        return BluetoothDeviceAdapter(this, listOf(fakeUseLastSelectedSpeakerDevice) + devicesInfo)
    }
}
