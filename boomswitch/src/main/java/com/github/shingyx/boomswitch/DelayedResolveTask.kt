package com.github.shingyx.boomswitch

import android.os.AsyncTask
import java.util.concurrent.CompletableFuture

class DelayedResolveTask<T>(
    private val completableFuture: CompletableFuture<T>,
    private val completeValue: T,
    private val delay: Long
) : AsyncTask<Unit, Unit, Unit>() {
    override fun doInBackground(vararg params: Unit?) {
        Thread.sleep(delay)
        completableFuture.complete(completeValue)
    }
}
