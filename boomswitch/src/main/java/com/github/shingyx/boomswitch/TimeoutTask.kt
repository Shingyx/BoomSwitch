package com.github.shingyx.boomswitch

import android.os.AsyncTask

class TimeoutTask(onTimedOut: () -> Unit, delay: Long) {
    private val task = TimeoutTaskInternal(onTimedOut, delay)

    fun start() {
        task.execute()
    }

    fun cancel() {
        task.cancel(true)
    }
}

private class TimeoutTaskInternal(
    private val onTimedOut: () -> Unit,
    private val delay: Long
) : AsyncTask<Unit, Unit, Unit>() {
    override fun doInBackground(vararg params: Unit?) {
        try {
            Thread.sleep(delay)
            onTimedOut()
        } catch (e: InterruptedException) {
        }
    }
}
