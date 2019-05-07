package com.github.shingyx.boomswitch

import android.os.AsyncTask

class TimeoutTask(
    private val onTimedOut: () -> Unit,
    private val timeout: Long
) : AsyncTask<Unit, Unit, Unit>() {
    override fun doInBackground(vararg params: Unit?) {
        try {
            Thread.sleep(timeout)
            onTimedOut()
        } catch (e: InterruptedException) {
        }
    }
}
