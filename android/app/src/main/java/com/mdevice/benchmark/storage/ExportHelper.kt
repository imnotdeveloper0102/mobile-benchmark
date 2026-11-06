package com.mdevice.benchmark.storage

import android.content.Context
import java.io.File
import java.io.FileWriter
import org.json.JSONArray
import org.json.JSONObject

/**
 * Xuat lich su ra CSV / JSON vao thu muc cache cua app de phia JS chia se
 * hoac luu file. Chi xuat nhung gi da luu trong Room, khong them truong nao.
 */
object ExportHelper {

    fun exportMetricsCsv(context: Context, metrics: List<MetricEntity>): File {
        val file = File(context.cacheDir, "metrics_export_${System.currentTimeMillis()}.csv")
        var writer: FileWriter? = null
        try {
            writer = FileWriter(file)
            writer.append("timestampMs,cpuLoadPct,cpuFreqAvgMHz,ramUsedBytes,ramTotalBytes,batteryTempC,cpuTempC,batteryLevelPct,charging,fps,rxBytesPerSec,txBytesPerSec\n")
            for (m in metrics) {
                writer.append(
                    listOf(
                        m.timestampMs.toString(),
                        m.cpuLoadPct?.toString() ?: "",
                        m.cpuFreqAvgMHz?.toString() ?: "",
                        m.ramUsedBytes.toString(),
                        m.ramTotalBytes.toString(),
                        m.batteryTempC?.toString() ?: "",
                        m.cpuTempC?.toString() ?: "",
                        m.batteryLevelPct.toString(),
                        m.charging.toString(),
                        m.fps.toString(),
                        m.rxBytesPerSec.toString(),
                        m.txBytesPerSec.toString(),
                    ).joinToString(","),
                )
                writer.append("\n")
            }
            writer.flush()
        } finally {
            try { writer?.close() } catch (_: Exception) { }
        }
        return file
    }

    fun exportBenchmarksJson(context: Context, results: List<BenchmarkResultEntity>): File {
        val file = File(context.cacheDir, "benchmarks_export_${System.currentTimeMillis()}.json")
        val array = JSONArray()
        for (r in results) {
            array.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("timestampMs", r.timestampMs)
                    put("cpuSingleMs", r.cpuSingleMs)
                    put("cpuMultiMs", r.cpuMultiMs)
                    put("sha256Ms", r.sha256Ms)
                    put("gzipMs", r.gzipMs)
                    put("matrixMs", r.matrixMs)
                    put("sortMs", r.sortMs)
                    put("memReadMBps", r.memReadMBps)
                    put("memWriteMBps", r.memWriteMBps)
                    put("randomLatencyNs", r.randomLatencyNs)
                    put("storageReadMBps", r.storageReadMBps)
                    put("storageWriteMBps", r.storageWriteMBps)
                    put("gpuFps", r.gpuFps)
                    put("gpuJankPct", r.gpuJankPct)
                    put("cpuScore", r.cpuScore)
                    put("memScore", r.memScore)
                    put("storageScore", r.storageScore)
                    put("gpuScore", r.gpuScore)
                    put("totalScore", r.totalScore)
                    if (r.stressDropPct != null) put("stressDropPct", r.stressDropPct)
                    put("deviceModel", r.deviceModel)
                },
            )
        }
        var writer: FileWriter? = null
        try {
            writer = FileWriter(file)
            writer.write(array.toString(2))
            writer.flush()
        } finally {
            try { writer?.close() } catch (_: Exception) { }
        }
        return file
    }
}
