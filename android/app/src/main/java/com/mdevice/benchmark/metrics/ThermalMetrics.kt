package com.mdevice.benchmark.metrics

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.mdevice.benchmark.util.SysFs

/**
 * Nhiet do.
 *
 * - Nhiet do pin: doc qua BatteryManager / ACTION_BATTERY_CHANGED, don vi
 *   chuan la 0.1 do C (EXTRA_TEMPERATURE). Day la nguon dang tin cay nhat.
 * - Nhiet do CPU/GPU: doc /sys/class/thermal/thermal_zoneN/{type,temp}.
 *   Ten zone khac nhau theo SoC (vd "cpu-0-0", "tsens_tz_sensor0", "gpu"),
 *   nen chi chon zone co ten chua tu khoa cpu/gpu va co gia tri hop le.
 *   Neu khong co zone nao khop thi tra null + ly do, khong doan mo.
 * - Thermal status: PowerManager.getCurrentThermalStatus, API 29+.
 */
class ThermalMetrics(private val context: Context) {

    data class Result(
        val batteryTempC: Double?,
        val cpuTempC: Double?,
        val gpuTempC: Double?,
        val thermalStatus: Int,
        val overheating: Boolean,
    )

    fun batteryTemperature(): Double? {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter) ?: return null
            val raw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            if (raw == Int.MIN_VALUE) null else raw / 10.0
        } catch (_: Exception) {
            null
        }
    }

    private fun readZones(): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        val base = "/sys/class/thermal"
        for (dir in SysFs.listChildren(base)) {
            if (!dir.name.startsWith("thermal_zone")) continue
            val type = SysFs.readText("${dir.absolutePath}/type") ?: continue
            val temp = SysFs.parseThermalTempMilliC(SysFs.readLong("${dir.absolutePath}/temp"))
            if (temp != null) out[type.lowercase()] = temp
        }
        return out
    }

    fun cpuTemperature(): Double? {
        val zones = readZones()
        val candidates = zones.entries.filter { (type, _) ->
            type.contains("cpu") || type.contains("tsens") || type.contains("soc") ||
                type.contains("big") || type.contains("little") || type.contains("cluster")
        }
        // Lay zone nong nhat trong nhom CPU vi do la diem nong dang chu y.
        return candidates.maxByOrNull { it.value }?.value
    }

    fun gpuTemperature(): Double? {
        val zones = readZones()
        val candidates = zones.entries.filter { (type, _) ->
            type.contains("gpu")
        }
        return candidates.maxByOrNull { it.value }?.value
    }

    fun thermalStatus(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return PowerManager.THERMAL_STATUS_NONE
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.currentThermalStatus
        } catch (_: Exception) {
            PowerManager.THERMAL_STATUS_NONE
        }
    }

    fun collect(): Result {
        val status = thermalStatus()
        val overheating = status >= PowerManager.THERMAL_STATUS_SEVERE
        return Result(
            batteryTempC = batteryTemperature(),
            cpuTempC = cpuTemperature(),
            gpuTempC = gpuTemperature(),
            thermalStatus = status,
            overheating = overheating,
        )
    }

    companion object {
        fun statusLabel(status: Int): String = when (status) {
            PowerManager.THERMAL_STATUS_NONE -> "Binh thuong"
            PowerManager.THERMAL_STATUS_LIGHT -> "Am nhe"
            PowerManager.THERMAL_STATUS_MODERATE -> "Am vua"
            PowerManager.THERMAL_STATUS_SEVERE -> "Nong"
            PowerManager.THERMAL_STATUS_CRITICAL -> "Rat nong"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "Khan cap"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "Sap tat may vi nhiet"
            else -> "Khong ro"
        }
    }
}
