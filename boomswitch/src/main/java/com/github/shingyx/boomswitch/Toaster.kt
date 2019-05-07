package com.github.shingyx.boomswitch

import android.app.Activity
import android.widget.Toast

class Toaster(private val activity: Activity) {
    private val toast = Toast.makeText(activity, "", Toast.LENGTH_LONG)

    fun showToast(text: String) {
        activity.runOnUiThread {
            toast.setText(text)
            toast.show()
        }
    }
}
