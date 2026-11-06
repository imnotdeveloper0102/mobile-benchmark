package com.mdevice.benchmark.metrics

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Pin.
 *
 * - Cong suat (W) la UOC TINH: P = U * I, voi U la dien ap (mV) va I la dong
 *   tuc thoi (uA, BATTERY_PROPERTY_CURRENT_NOW). Tren nhieu may dong nay tra
 *   ve 0 hoac gia tri am khi dang xa; khi do powerWatts = null.
 * - Thoi gian con lai chi la uoc tinh tho: dung luong con lai (mAh uoc tinh
 *   tu % pin va dung luong danh dinh neu doc duoc) chia cho dong xa hien tai.
 *   Neu dang sac hoac khong doc duoc dong thi tra null.
 * - Suc khoe pin lay tu EXTRA_HEALTH cua ACTION_BATTERY_CHANGED.
 */
class BatteryMetrics(private val context: Context) {

    data class Result(
        val levelPct: Int,
        val charging: Boolean,
        val currentNowUa: Long?,
        val voltageMv: Int?,
        val powerWatts: Double?,
        val health: String,
        val timeRemainingMin: Double?,
    )

    fun collect(): Result {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = try { context.registerReceiver(null, filter) } catch (_: Exception) { null }

        val level = intent?.let {
            val lvl = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (lvl >= 0 && scale > 0) (lvl * 100 / scale) else -1
        } ?: -1
        val levelPct = if (level < 0) {
            try {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            } catch (_: Exception) { -1 }
        } else level

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val currentNow = try {
            val value = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (value == Long.MIN_VALUE) null else value
        } catch (_: Exception) { null }

        val voltageMv = intent?.let {
            val v = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            if (v > 0) v else null
        }

        val powerWatts = if (currentNow != null && voltageMv != null && currentNow != 0L) {
            // uA * mV = nW; doi sang W.
            kotlin.math.abs(currentNow.toDouble() * voltageMv.toDouble()) / 1_000_000_000.0
        } else null

        val healthCode = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Tot"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Qua nhiet"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Hong"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Qua ap"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Loi khong xac dinh"
            BatteryManager.BATTERY_HEALTH_COLD -> "Qua lanh"
            else -> "Khong ro"
        }

        val timeRemainingMin = estimateTimeRemaining(levelPct, currentNow, charging)
        return Result(
            levelPct = levelPct,
            charging = charging,
            currentNowUa = currentNow,
            voltageMv = voltageMv,
            powerWatts = powerWatts,
            health = health,
            timeRemainingMin = timeRemainingMin,
        )
    }

    private fun estimateTimeRemaining(levelPct: Int, currentNowUa: Long?, charging: Boolean): Double? {
        if (charging) return null
        if (currentNowUa == null || currentNowUa >= 0) return null
        if (levelPct < 0 || levelPct > 100) return null
        // Can dong xa (mA) va uoc tinh dung luong con lai theo % pin.
        // Khong co dung luong danh dinh chuan tu API cong khai, nen chi uoc
        // tinh tu dong hien tai va gia dinh pin con lai ti le voi %.
        // Neu dong xa qua nho (< 50 mA) thi uoc tinh khong on dinh -> null.
        val dischargeMa = -currentNowUa / 1000.0
        if (dischargeMa < 50.0) return null
        // Uoc tinh tho: gia su pin day ~4000 mAh (gia tri tham khao de tinh
        // ti le, khong phai dung luong that cua may). Ket qua chi mang tinh
        // tham khao va phia UI phai ghi ro nhu vay.
        val remainingMah = 4000.0 * levelPct / 100.0
        val hours = remainingMah / dischargeMa
        if (hours <= 0.0 || hours > 48.0) return null
        return hours * 60.0
    }
}
