package com.github.shingyx.boomswitch

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.RemoteViews
import android.widget.Toast

private const val BOOM_SWITCH = "BOOM_SWITCH"

class MainWidget : AppWidgetProvider() {
    private var initialized = false
    private lateinit var handler: Handler
    private var toast: Toast? = null

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.main_widget)

            val intent = Intent(BOOM_SWITCH, null, context, this.javaClass)
            views.setOnClickPendingIntent(
                R.id.widgetLayout,
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == BOOM_SWITCH) {
            lazySetup(context)

            BoomClient.switchPower(context) { progressMessage ->
                reportProgress(context, progressMessage)
            }
        }
    }

    private fun lazySetup(context: Context) {
        if (!initialized) {
            Preferences.initialize(context)
            handler = Handler()
            initialized = true
        }
    }

    private fun reportProgress(context: Context, message: String) {
        handler.post {
            toast?.cancel()
            toast = Toast.makeText(context, message, Toast.LENGTH_LONG).also {
                it.show()
            }
        }
    }
}
