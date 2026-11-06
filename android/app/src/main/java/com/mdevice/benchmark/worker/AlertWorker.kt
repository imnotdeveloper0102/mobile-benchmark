package com.mdevice.benchmark.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mdevice.benchmark.bridge.MetricsCollector
import com.mdevice.benchmark.util.AlertChecker
import java.util.concurrent.TimeUnit

/**
 * Worker kiem tra canh bao dinh ky (15 phut - gioi han toi thieu cua
 * WorkManager cho PeriodicWork). Khong chay lien tuc nhu service vi
 * canh bao chi can kiem tra theo chu ky; lay mau realtime van do
 * MetricsService dam nhan.
 */
class AlertWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val CHANNEL_ID = "benchmark_performance_alerts"
        const val WORK_NAME = "benchmark_performance_alerts"
        const val NOTIFICATION_ID_BASE = 2000

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AlertWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val collector = MetricsCollector(applicationContext)
            val snapshot = collector.collectSnapshot()
            collector.stop()
            val alerts = AlertChecker.check(applicationContext, snapshot)
            for (alert in alerts) {
                showNotification(alert)
            }
            // Don dep du lieu cu > 30 ngay trong cung luot chay dinh ky.
            try {
                val db = com.mdevice.benchmark.storage.AppDatabase.get(applicationContext)
                val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                db.metricDao().deleteOlderThan(cutoff)
            } catch (_: Exception) { }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(alert: AlertChecker.Alert) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Canh bao hieu nang",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Canh bao nhiet do, RAM thap, pin yeu" }
                manager.createNotificationChannel(channel)
            }
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_BASE + alert.key.hashCode(), notification)
    }
}
