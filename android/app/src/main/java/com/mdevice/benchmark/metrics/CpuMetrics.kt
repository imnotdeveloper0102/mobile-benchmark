package com.mdevice.benchmark.metrics

import android.os.Process
import android.os.SystemClock
import com.mdevice.benchmark.util.SysFs

/**
 * So lieu CPU.
 *
 * Gioi han tren Android 8+ (API 26): /proc/stat cua he thong thuong bi chan,
 * nen khong doc duoc ti le tai CPU thuc cua toan he thong. Vi vay muc tai o
 * day la UOC TINH, suy ra tu hai nguon hop phap:
 *  (1) ti le tan so hien tai / tan so toi da trung binh cua cac nhan dang online,
 *  (2) thoi gian CPU cua chinh tien trinh app (Process.getElapsedCpuTime).
 * Snapshot phai ghi loadMethod = "freq+procCpuTime" va ghi chu ro day chi la
 * uoc tinh, khong phai tai thuc cua he thong. Khi ca hai nguon deu khong co,
 * loadEstimatePct = null va loadMethod = "unavailable".
 */
class CpuMetrics {

    data class Result(
        val cores: Int,
        val freqMHz: List<Double?>,
        val maxFreqMHz: List<Double?>,
        val loadEstimatePct: Double?,
        val loadMethod: String,
        val loadNote: String,
    )

    private var lastProcCpuMs: Long = -1L
    private var lastWallMs: Long = -1L
    private var lastProcStat: SysFs.ProcStatCpu? = null

    fun coreCount(): Int {
        return try {
            Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        } catch (_: Exception) {
            1
        }
    }

    fun currentFrequencies(cores: Int): List<Double?> {
        return (0 until cores).map { index ->
            val path = "/sys/devices/system/cpu/cpu$index/cpufreq/scaling_cur_freq"
            SysFs.parseFreqKhzToMhz(SysFs.readLong(path))
        }
    }

    fun maxFrequencies(cores: Int): List<Double?> {
        return (0 until cores).map { index ->
            val path = "/sys/devices/system/cpu/cpu$index/cpufreq/cpuinfo_max_freq"
            SysFs.parseFreqKhzToMhz(SysFs.readLong(path))
        }
    }

    fun collect(): Result {
        val cores = coreCount()
        val freq = currentFrequencies(cores)
        val maxFreq = maxFrequencies(cores)
        val (load, method, note) = estimateLoad(freq, maxFreq)
        return Result(
            cores = cores,
            freqMHz = freq,
            maxFreqMHz = maxFreq,
            loadEstimatePct = load,
            loadMethod = method,
            loadNote = note,
        )
    }

    private fun estimateLoad(
        freq: List<Double?>,
        maxFreq: List<Double?>,
    ): Triple<Double?, String, String> {
        // Nguon 1: /proc/stat, chi dung duoc tren mot so thiet bi / ROM cho phep.
        val stat = SysFs.readProcStatCpuLine()
        val previousStat = lastProcStat
        lastProcStat = stat
        if (stat != null && previousStat != null) {
            val load = stat.loadSince(previousStat)
            if (load != null) {
                return Triple(
                    load.coerceIn(0.0, 100.0),
                    "freq+procCpuTime",
                    "Tai he thong doc tu /proc/stat (chi kha dung tren thiet bi cho phep).",
                )
            }
        }

        // Nguon 2: ti le tan so trung binh.
        val pairs = freq.zip(maxFreq).mapNotNull { (f, m) ->
            if (f != null && m != null && m > 0) f / m else null
        }
        val freqRatio = if (pairs.isNotEmpty()) pairs.average() else null

        // Nguon 3: thoi gian CPU cua chinh tien trinh tren khoang lay mau.
        val nowWall = SystemClock.elapsedRealtime()
        val nowProc = try { Process.getElapsedCpuTime() } catch (_: Exception) { -1L }
        var procLoad: Double? = null
        if (lastProcCpuMs >= 0 && lastWallMs >= 0 && nowProc >= 0 && nowWall > lastWallMs) {
            val procDelta = (nowProc - lastProcCpuMs).toDouble()
            val wallDelta = (nowWall - lastWallMs).toDouble()
            val cores = coreCount().toDouble()
            if (wallDelta > 0 && cores > 0) {
                procLoad = (procDelta / wallDelta) / cores * 100.0
            }
        }
        lastProcCpuMs = nowProc
        lastWallMs = nowWall

        if (freqRatio != null) {
            // Trong so: tan so phan anh muc tai chung cua he thong, thoi gian CPU
            // cua app chi la thanh phan nho. Ghi ro day la uoc tinh.
            val estimate = (freqRatio * 100.0).coerceIn(0.0, 100.0)
            val note = if (procLoad != null) {
                "Uoc tinh tu ti le tan so trung binh (%.1f%%) va thoi gian CPU cua chinh app (%.1f%%). Khong phai tai thuc cua he thong vi Android 8+ chan /proc/stat."
                    .format(freqRatio * 100.0, procLoad.coerceIn(0.0, 100.0))
            } else {
                "Uoc tinh tu ti le tan so trung binh. Khong phai tai thuc cua he thong vi Android 8+ chan /proc/stat."
            }
            return Triple(estimate, "freq+procCpuTime", note)
        }
        if (procLoad != null) {
            return Triple(
                procLoad.coerceIn(0.0, 100.0),
                "freq+procCpuTime",
                "Chi uoc tinh duoc phan CPU cua chinh app (Process.getElapsedCpuTime); tan so nhan khong doc duoc.",
            )
        }
        return Triple(
            null,
            "unavailable",
            "Khong doc duoc tan so nhan (/sys/.../cpufreq) va /proc/stat bi chan tren Android 8+.",
        )
    }
}
