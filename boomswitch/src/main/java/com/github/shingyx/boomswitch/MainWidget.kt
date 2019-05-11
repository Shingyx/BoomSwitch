package com.github.shingyx.boomswitch

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.RemoteViews

private const val BOOM_SWITCH = "BOOM_SWITCH"

class MainWidget : AppWidgetProvider() {
    private var initialized = false
    private lateinit var handler: Handler
    private lateinit var toaster: Toaster

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val intent = Intent(context, this.javaClass)
            intent.action = BOOM_SWITCH
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val views = RemoteViews(context.packageName, R.layout.main_widget)
            views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == BOOM_SWITCH) {
            lazySetup(context)

            BoomClient.switchPower(context) { toaster.show(it) }
        }
    }

    private fun lazySetup(context: Context) {
        if (!initialized) {
            Preferences.initialize(context)
            handler = Handler()
            toaster = Toaster(context, handler)
            initialized = true
        }
    }
}
