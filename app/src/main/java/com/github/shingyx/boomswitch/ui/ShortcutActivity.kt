package com.github.shingyx.boomswitch.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.boomswitch.R
import com.github.shingyx.boomswitch.data.BluetoothDeviceInfo
import com.github.shingyx.boomswitch.data.BoomClient
import com.github.shingyx.boomswitch.data.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

class ShortcutActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish() // Finish asap, or multiple tasks could appear

        if (intent.action == ACTION_SWITCH) {
            val deviceInfo = BluetoothDeviceInfo.createFromIntent(intent)
                ?: Preferences.bluetoothDeviceInfo

            if (deviceInfo == null) {
                updateToast(getString(R.string.select_speaker))
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            } else {
                launch { switchBoom(deviceInfo) }
            }
        } else {
            Timber.w("Unexpected intent action ${intent.action}")
        }
    }

    private suspend fun switchBoom(deviceInfo: BluetoothDeviceInfo) {
        BoomClient.switchPower(this, deviceInfo) { progressMessage ->
            runOnUiThread {
                updateToast(progressMessage)
            }
        }
    }

    private fun updateToast(text: String) {
        toast?.cancel()
        toast = Toast.makeText(this, text, Toast.LENGTH_LONG).also {
            it.show()
        }
    }

    companion object {
        const val ACTION_SWITCH = "com.github.shingyx.boomswitch.SWITCH"

        private var toast: Toast? = null

        fun createShortcutIntent(
            context: Context,
            bluetoothDeviceInfo: BluetoothDeviceInfo?
        ): Intent {
            val shortcutIntent = Intent(ACTION_SWITCH, null, context, ShortcutActivity::class.java)
            bluetoothDeviceInfo?.addToIntent(shortcutIntent)
            val shortcutName = bluetoothDeviceInfo?.name ?: context.getString(R.string.app_name)
            val iconRes = Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher)

            @Suppress("DEPRECATION") // Use deprecated approach for no icon badge
            return Intent().apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName)
                putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes)
            }
        }
    }
}
