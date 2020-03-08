package com.github.shingyx.boomswitch.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

class CreateSimpleShortcutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == Intent.ACTION_CREATE_SHORTCUT) {
            val intent = ShortcutActivity.createShortcutIntent(this, null)
            setResult(Activity.RESULT_OK, intent)
        } else {
            Timber.w("Unexpected intent action ${intent.action}")
        }

        finish()
    }
}
