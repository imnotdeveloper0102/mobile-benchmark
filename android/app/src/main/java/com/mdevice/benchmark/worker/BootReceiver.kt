package com.mdevice.benchmark.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mdevice.benchmark.service.MetricsService

/**
 * Khoi dong lai lay mau sau khi may khoi dong, CHI khi nguoi dung da bat
 * tuy chon "khoi dong lai sau khi reboot" trong Cai dat (prefs
 * restart_on_boot). Mac dinh tat.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(MetricsService.PREFS_NAME, Context.MODE_PRIVATE)
        val restartOnBoot = prefs.getBoolean(MetricsService.PREF_RESTART_ON_BOOT, false)
        val samplingEnabled = prefs.getBoolean(MetricsService.PREF_SAMPLING_ENABLED, false)
        if (!restartOnBoot || !samplingEnabled) return
        val serviceIntent = Intent(context, MetricsService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {
            // He thong co the chan khoi dong foreground service ngay sau boot
            // tren mot so ROM; truong hop nay nguoi dung mo app len se bat lai.
        }
    }
}
