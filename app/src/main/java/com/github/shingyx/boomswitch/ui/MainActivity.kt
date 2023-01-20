package com.github.shingyx.boomswitch.ui

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.boomswitch.BuildConfig
import com.github.shingyx.boomswitch.R
import com.github.shingyx.boomswitch.data.AppColorTheme
import com.github.shingyx.boomswitch.data.BoomClient
import com.github.shingyx.boomswitch.data.Preferences
import com.github.shingyx.boomswitch.databinding.ActivityMainBinding
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var handler: Handler
    private lateinit var adapter: BluetoothDeviceAdapter
    private lateinit var bluetoothStateReceiver: BluetoothStateReceiver

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                updateBluetoothDevices()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.bluetooth_missing_permission_alert)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        updateBluetoothDevices()
                    }
                    .show()
            }
        }

    private val bluetoothOffAlertDialog = lazy {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.bluetooth_turned_off_alert)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        handler = Handler(Looper.getMainLooper())
        adapter = BluetoothDeviceAdapter(this)
        bluetoothStateReceiver = BluetoothStateReceiver(this::updateBluetoothDevices)

        binding.selectSpeaker.setAdapter(adapter)
        binding.selectSpeaker.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            Preferences.bluetoothDeviceInfo = adapter.getItem(pos)
            binding.switchButton.isEnabled = true
        }
        binding.selectSpeaker.setText(Preferences.bluetoothDeviceInfo?.toString())
        binding.selectSpeaker.requestFocus()

        binding.switchButton.isEnabled = Preferences.bluetoothDeviceInfo != null
        binding.switchButton.setOnClickListener { launch { switchBoom() } }

        binding.version.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        registerReceiver(bluetoothStateReceiver, BluetoothStateReceiver.intentFilter())

        if (!BoomClient.hasBluetoothConnectPermission(this)) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.selectSpeaker.dismissDropDown()
        if (BoomClient.hasBluetoothConnectPermission(this)) {
            updateBluetoothDevices()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothStateReceiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.choose_theme -> chooseTheme()
            R.id.open_source_licenses -> showOpenSourceLicenses()
            R.id.send_feedback -> sendFeedback()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private suspend fun switchBoom() {
        val deviceInfo = Preferences.bluetoothDeviceInfo
            ?: return Toast.makeText(this, R.string.select_speaker, Toast.LENGTH_LONG).show()

        handler.removeCallbacksAndMessages(null)

        binding.switchButton.isEnabled = false
        fadeView(binding.progressBar, true)
        binding.progressDescription.text = ""
        fadeView(binding.progressDescription, true)

        BoomClient.switchPower(this, deviceInfo) { progressMessage ->
            runOnUiThread {
                binding.progressDescription.text = progressMessage
            }
        }

        binding.switchButton.isEnabled = true
        fadeView(binding.progressBar, false)
        handler.postDelayed({
            fadeView(binding.progressDescription, false)
        }, 10000)
    }

    private fun fadeView(view: View, show: Boolean) {
        val newAlpha = if (show) 1f else 0f
        view.visibility = View.VISIBLE
        view.alpha = 1f - newAlpha
        view.animate()
            .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
            .alpha(newAlpha)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = if (show) View.VISIBLE else View.GONE
                }
            })
    }

    private fun updateBluetoothDevices() {
        var devicesInfo = BoomClient.getPairedDevicesInfo()

        if (devicesInfo == null) {
            if (!BoomClient.hasBluetoothConnectPermission(this)) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                bluetoothOffAlertDialog.value.show()
            }
            devicesInfo = emptyList()
        } else {
            if (bluetoothOffAlertDialog.isInitialized()) {
                bluetoothOffAlertDialog.value.dismiss()
            }
        }

        binding.selectSpeakerContainer.error = if (devicesInfo.isEmpty()) {
            getString(R.string.no_devices_found)
        } else {
            null
        }

        adapter.updateItems(devicesInfo)
    }

    private fun chooseTheme() {
        val themes = AppColorTheme.values()
        val themeNames = themes.map { getString(it.descriptionResId) }.toTypedArray()
        val currentThemeIndex = themes.indexOf(Preferences.appColorTheme)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_theme)
            .setSingleChoiceItems(themeNames, currentThemeIndex) { dialog, i ->
                dialog.dismiss()
                val selectedTheme = themes[i]
                Preferences.appColorTheme = selectedTheme
                selectedTheme.apply()
            }
            .show()
    }

    private fun showOpenSourceLicenses() {
        OssLicensesMenuActivity.setActivityTitle(getString(R.string.open_source_licenses))
        startActivity(Intent(this, OssLicensesMenuActivity::class.java))
    }

    private fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("shingyx.dev@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.send_feedback_email_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.send_feedback_email_body))
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.error_no_email_client, Toast.LENGTH_LONG).show()
        }
    }
}
