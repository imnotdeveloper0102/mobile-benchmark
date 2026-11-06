package com.mdevice.benchmark.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mdevice.benchmark.bridge.MetricsCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * ForegroundService lay mau lien tuc.
 *
 * - Mac dinh 1 Hz (1000 ms). Khi man hinh tat (SCREEN_OFF) giam xuong 0.2 Hz
 *   (5000 ms) de tiet kiem pin va tranh tu lam nong may; khi man hinh bat
 *   lai thi tro ve 1 Hz.
 * - Moi 750 ms gui 1 batch ve phia JS qua LocalBroadcast
 *   (action ACTION_METRICS_BATCH, extra "batch" la chuoi JSON array, moi
 *   batch chi chua 1 snapshot - khop hop dong bridge). Service khong gui
 *   tung mau le qua bridge de tranh nghen.
 * - Notification co nut Dung / Tam dung, bam nut se gui broadcast nguoc lai
 *   cho service xu ly.
 */
class MetricsService : Service() {

    companion object {
        const val CHANNEL_ID = "benchmark_performance_metrics"
        const val NOTIFICATION_ID = 1001
        const val ACTION_METRICS_BATCH = "com.mdevice.benchmark.METRICS_BATCH"
        const val ACTION_STOP = "com.mdevice.benchmark.METRICS_STOP"
        const val ACTION_PAUSE = "com.mdevice.benchmark.METRICS_PAUSE"
        const val ACTION_RESUME = "com.mdevice.benchmark.METRICS_RESUME"
        const val EXTRA_BATCH = "batch"
        const val PREFS_NAME = "benchmark_performance_prefs"
        const val PREF_SAMPLING_ENABLED = "sampling_enabled"
        const val PREF_RESTART_ON_BOOT = "restart_on_boot"
        const val PREF_INTERVAL_MS = "interval_ms"
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var samplingJob: Job? = null
    private var collector: MetricsCollector? = null
    private var paused = false
    private var screenOn = true
    private var intervalMs: Long = 1000L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    restartSampling()
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    restartSampling()
                }
            }
        }
    }

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_STOP -> stopSelf()
                ACTION_PAUSE -> {
                    paused = true
                    updateNotification()
                }
                ACTION_RESUME -> {
                    paused = false
                    updateNotification()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        collector = MetricsCollector(applicationContext)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        intervalMs = prefs.getLong(PREF_INTERVAL_MS, 1000L).coerceIn(500L, 5000L)
        createChannel()
        ContextCompat.registerReceiver(
            this, screenReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF).apply {
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            controlReceiver,
            IntentFilter(ACTION_STOP).apply {
                addAction(ACTION_PAUSE)
                addAction(ACTION_RESUME)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_PAUSE) paused = true
        if (intent?.action == ACTION_RESUME) paused = false
        restartSampling()
        return START_STICKY
    }

    private fun effectiveIntervalMs(): Long {
        return if (screenOn) intervalMs else 5000L
    }

    private fun restartSampling() {
        samplingJob?.cancel()
        samplingJob = scope.launch {
            while (isActive) {
                val delayMs = effectiveIntervalMs()
                if (!paused) {
                    val snapshot = collector?.collectSnapshot()
                    if (snapshot != null) {
                        sendBatch(snapshot)
                    }
                }
                delay(delayMs)
            }
        }
    }

    private fun sendBatch(snapshot: JSONObject) {
        // Hop dong bridge: batch 750ms, moi batch 1 snapshot. Service lay mau
        // 1Hz; bridge phia JS se gom lai theo chu ky 750ms cua no. O day gui
        // ngay sau moi lan lay mau de khong mat mau khi man hinh tat.
        val array = JSONArray().put(snapshot)
        val intent = Intent(ACTION_METRICS_BATCH).putExtra(EXTRA_BATCH, array.toString())
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Theo doi hieu nang",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Thong bao khi dang lay mau hieu nang nen"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MetricsService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val toggleIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MetricsService::class.java)
                .setAction(if (paused) ACTION_RESUME else ACTION_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Benchmark Performance")
            .setContentText(if (paused) "Da tam dung lay mau" else "Dang theo doi hieu nang")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, if (paused) "Tiep tuc" else "Tam dung", toggleIntent)
            .addAction(android.R.drawable.ic_delete, "Dung", stopIntent)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        samplingJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) { }
        try { LocalBroadcastManager.getInstance(this).unregisterReceiver(controlReceiver) } catch (_: Exception) { }
        collector?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
