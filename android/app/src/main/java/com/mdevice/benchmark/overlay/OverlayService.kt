package com.mdevice.benchmark.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.mdevice.benchmark.bridge.MetricsCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Service overlay noi (SYSTEM_ALERT_WINDOW).
 * Hien thi tren moi app, cap nhat 1 Hz. Khong tu xin quyen: phia JS phai
 * kiem tra Settings.canDrawOverlays truoc roi moi goi startOverlay.
 */
class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "benchmark_performance_overlay"
        const val NOTIFICATION_ID = 1002
        const val EXTRA_CONFIG = "config"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: OverlayView? = null
    private var params: WindowManager.LayoutParams? = null
    private var collector: MetricsCollector? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        collector = MetricsCollector(applicationContext)
        createChannel()
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
        val configJson = intent?.getStringExtra(EXTRA_CONFIG) ?: "{}"
        val config = try { JSONObject(configJson) } catch (_: Exception) { JSONObject() }
        if (overlayView == null) {
            val view = OverlayView(this)
            view.applyConfig(config)
            view.onDrag = { x, y ->
                params?.let { p ->
                    p.x = x
                    p.y = y
                    try { windowManager?.updateViewLayout(view, p) } catch (_: Exception) { }
                }
            }
            params = OverlayView.layoutParams()
            try {
                windowManager?.addView(view, params)
                overlayView = view
            } catch (_: Exception) {
                stopSelf()
                return START_NOT_STICKY
            }
        } else {
            overlayView?.applyConfig(config)
        }
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                val snapshot = collector?.collectSnapshot()
                if (snapshot != null) {
                    val view = overlayView
                    if (view != null) {
                        view.post { view.update(snapshot) }
                    }
                }
                delay(1000L)
            }
        }
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Overlay hieu nang",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Thong bao khi overlay noi dang bat" }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Benchmark Performance")
            .setContentText("Overlay hieu nang dang bat")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        updateJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        overlayView?.let { view ->
            try { windowManager?.removeView(view) } catch (_: Exception) { }
        }
        overlayView = null
        collector?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
