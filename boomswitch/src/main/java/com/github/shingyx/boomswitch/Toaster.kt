package com.github.shingyx.boomswitch

import android.content.Context
import android.widget.Toast

class Toaster(
    private val context: Context,
    private val fnWrapper: (() -> Unit) -> Unit
) {
    private var toast = Toast.makeText(context, "", Toast.LENGTH_LONG)

    fun show(text: String) {
        fnWrapper {
            toast.cancel()
            toast = Toast.makeText(context, text, Toast.LENGTH_LONG)
            toast.show()
        }
    }
}
