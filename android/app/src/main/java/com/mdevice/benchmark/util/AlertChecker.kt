package com.mdevice.benchmark.util

import android.content.Context
import org.json.JSONObject

/**
 * Kiem tra nguong canh bao tren 1 snapshot.
 * Mac dinh: nhiet pin > 45 do C, RAM trong < 10%, pin < 15%.
 * Nguong co the doi qua bridge setAlertThresholds (luu vao prefs
 * "benchmark_alerts"). Tat ca deu bat/tat duoc qua co "enabled".
 */
object AlertChecker {

    data class Alert(val key: String, val title: String, val message: String)

    data class Thresholds(
        val tempC: Float,
        val ramAvailPct: Float,
        val batteryPct: Float,
        val enabled: Boolean,
    )

    fun loadThresholds(context: Context): Thresholds {
        val prefs = context.getSharedPreferences("benchmark_alerts", Context.MODE_PRIVATE)
        return Thresholds(
            tempC = prefs.getFloat("tempC", 45f),
            ramAvailPct = prefs.getFloat("ramAvailPct", 10f),
            batteryPct = prefs.getFloat("batteryPct", 15f),
            enabled = prefs.getBoolean("enabled", true),
        )
    }

    fun check(context: Context, snapshot: JSONObject): List<Alert> {
        val thresholds = loadThresholds(context)
        if (!thresholds.enabled) return emptyList()
        val alerts = mutableListOf<Alert>()
        val thermal = snapshot.optJSONObject("thermal")
        val batteryTemp = thermal?.optDouble("batteryTempC", Double.NaN)
        if (batteryTemp != null && !batteryTemp.isNaN() && batteryTemp > thresholds.tempC) {
            alerts.add(
                Alert(
                    key = "temp",
                    title = "Canh bao nhiet do",
                    message = "Nhiet do pin %.1f°C vuot nguong %.0f°C.".format(batteryTemp, thresholds.tempC),
                ),
            )
        }
        val memory = snapshot.optJSONObject("memory")
        if (memory != null) {
            val total = memory.optLong("totalBytes", 0L)
            val avail = memory.optLong("availBytes", 0L)
            if (total > 0) {
                val availPct = avail * 100.0 / total
                if (availPct < thresholds.ramAvailPct) {
                    alerts.add(
                        Alert(
                            key = "ram",
                            title = "RAM sap het",
                            message = "RAM trong chi con %.1f%% (nguong %.0f%%).".format(availPct, thresholds.ramAvailPct),
                        ),
                    )
                }
            }
        }
        val battery = snapshot.optJSONObject("battery")
        if (battery != null) {
            val level = battery.optInt("levelPct", -1)
            val charging = battery.optBoolean("charging", false)
            if (level >= 0 && level < thresholds.batteryPct && !charging) {
                alerts.add(
                    Alert(
                        key = "battery",
                        title = "Pin yeu",
                        message = "Pin chi con %d%% (nguong %.0f%%).".format(level, thresholds.batteryPct),
                    ),
                )
            }
        }
        return alerts
    }
}
