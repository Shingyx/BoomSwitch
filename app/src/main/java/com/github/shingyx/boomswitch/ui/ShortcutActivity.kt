package com.github.shingyx.boomswitch.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.shingyx.boomswitch.R
import com.github.shingyx.boomswitch.data.BoomClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val ACTION_BOOM_SWITCH = "ACTION_BOOM_SWITCH"

private var toast: Toast? = null

class ShortcutActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private val tag = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            Intent.ACTION_CREATE_SHORTCUT -> createShortcut()
            ACTION_BOOM_SWITCH -> launch { switchBoom() }
            else -> Log.w(tag, "Unknown intent action ${intent.action}")
        }

        finish()
    }

    private fun createShortcut() {
        val shortcutIntent = Intent(ACTION_BOOM_SWITCH, null, this, javaClass)
        val iconResource = Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher)
        @Suppress("DEPRECATION") // Use deprecated approach for no icon badge
        val intent = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name))
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)
        }
        setResult(Activity.RESULT_OK, intent)
    }

    private suspend fun switchBoom() {
        BoomClient.switchPower(this) { progressMessage ->
            runOnUiThread {
                toast?.cancel()
                toast = Toast.makeText(this, progressMessage, Toast.LENGTH_LONG).also {
                    it.show()
                }
            }
        }
    }
}
