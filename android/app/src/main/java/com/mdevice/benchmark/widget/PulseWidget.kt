package com.mdevice.benchmark.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mdevice.benchmark.bridge.MetricsCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget man hinh chinh (RemoteViews): RAM / pin / nhiet.
 *
 * Gioi han: WorkManager PeriodicWork toi thieu 15 phut, nen widget chi
 * cap nhat toi da moi 15 phut (khong phai realtime). Cap nhat duoc kich
 * hoat bang cach goi requestUpdate() tu AlertWorker hoac tu app, hoac
 * he thong goi onUpdate() theo updatePeriodMillis trong widget info.
 */
class PulseWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE = "com.mdevice.benchmark.widget.UPDATE"

        fun requestUpdate(context: Context) {
            val intent = Intent(context, PulseWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(
                        android.content.ComponentName(context, PulseWidget::class.java),
                    )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (widgetId in appWidgetIds) {
            updateOne(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE) {
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, PulseWidget::class.java),
            )
            val manager = AppWidgetManager.getInstance(context)
            for (id in ids) updateOne(context, manager, id)
        }
    }

    private fun updateOne(context: Context, manager: AppWidgetManager, widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val collector = MetricsCollector(context.applicationContext)
            val snapshot = try { collector.collectSnapshot() } finally { collector.stop() }
            val memory = snapshot.optJSONObject("memory")
            val battery = snapshot.optJSONObject("battery")
            val thermal = snapshot.optJSONObject("thermal")
            val usedGb = (memory?.optLong("usedBytes", 0L) ?: 0L) / 1_073_741_824.0
            val totalGb = (memory?.optLong("totalBytes", 0L) ?: 0L) / 1_073_741_824.0
            val level = battery?.optInt("levelPct", -1) ?: -1
            val temp = thermal?.optDouble("batteryTempC", Double.NaN) ?: Double.NaN
            val views = RemoteViews(context.packageName, com.mdevice.benchmark.R.layout.widget_pulse)
            views.setTextViewText(
                com.mdevice.benchmark.R.id.widget_ram,
                "RAM %.1f/%.1f GB".format(usedGb, totalGb),
            )
            views.setTextViewText(
                com.mdevice.benchmark.R.id.widget_battery,
                "Pin " + if (level >= 0) "$level%" else "khong kha dung",
            )
            views.setTextViewText(
                com.mdevice.benchmark.R.id.widget_temp,
                "Nhiet " + if (!temp.isNaN()) "%.1f°C".format(temp) else "khong kha dung",
            )
            val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (openIntent != null) {
                val pending = PendingIntent.getActivity(
                    context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(com.mdevice.benchmark.R.id.widget_title, pending)
            }
            try { manager.updateAppWidget(widgetId, views) } catch (_: Exception) { }
        }
    }
}
