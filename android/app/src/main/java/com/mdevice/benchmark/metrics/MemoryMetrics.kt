package com.mdevice.benchmark.metrics

import android.app.ActivityManager
import android.content.Context
import android.os.Debug

class MemoryMetrics(private val context: Context) {

    data class Result(
        val totalBytes: Long,
        val availBytes: Long,
        val usedBytes: Long,
        val lowMemory: Boolean,
        val thresholdBytes: Long,
        val appPssBytes: Long,
    )

    fun collect(): Result {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        val total = info.totalMem
        val avail = info.availMem
        val used = (total - avail).coerceAtLeast(0L)
        val appPssKb = try {
            Debug.getPss().toLong()
        } catch (_: Exception) {
            0L
        }
        return Result(
            totalBytes = total,
            availBytes = avail,
            usedBytes = used,
            lowMemory = info.lowMemory,
            thresholdBytes = info.threshold,
            appPssBytes = appPssKb * 1024L,
        )
    }
}
