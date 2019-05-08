package com.github.shingyx.boomswitch

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.widget.RemoteViews

private val TAG = MainWidget::class.java.simpleName

private const val BOOM_SWITCH = "BOOM_SWITCH"

class MainWidget : AppWidgetProvider() {
    private lateinit var handler: Handler
    private lateinit var toaster: Toaster

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.main_widget)

            // Add the click listener
            val intent = Intent(context, this.javaClass)
            intent.action = BOOM_SWITCH
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == BOOM_SWITCH) {
            lazySetup(context)

            try {
                // TODO these need to be moved to a new view to avoid the try-catch
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                val pairedDevices = bluetoothAdapter.bondedDevices
                val boomDevice = pairedDevices.first()
                val deviceInfo = BluetoothDeviceInfo(boomDevice)
                BoomClient.switchPower(context, deviceInfo) {
                    toaster.show(it)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get device", e)
                toaster.show(e.message ?: "Unknown error.")
            }
        }
    }

    private fun lazySetup(context: Context) {
        if (!this::handler.isInitialized) {
            handler = Handler()
        }
        if (!this::toaster.isInitialized) {
            toaster = Toaster(context) { handler.post(it) }
        }
    }
}
