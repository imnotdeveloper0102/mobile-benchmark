package com.mdevice.benchmark.util

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Doc file /sys, /proc mot cach an toan.
 *
 * Moi thao tac doc deu boc try/catch rieng: mot file khong doc duoc (khong ton
 * tai, khong co quyen, SELinux chan) thi tra null thay vi lam do ca lan lay mau.
 * Nguoi goi phai xu ly null bang cach ghi "khong kha dung" kem ly do, khong
 * duoc tu dien gia tri thay the.
 */
object SysFs {

    fun readText(path: String): String? {
        var reader: BufferedReader? = null
        return try {
            reader = BufferedReader(FileReader(File(path)))
            reader.readLine()?.trim()
        } catch (_: Exception) {
            null
        } finally {
            try { reader?.close() } catch (_: Exception) { }
        }
    }

    fun readLong(path: String): Long? {
        val text = readText(path) ?: return null
        return text.toLongOrNull()
    }

    fun readInt(path: String): Int? {
        val text = readText(path) ?: return null
        return text.toIntOrNull()
    }

    fun readAllLines(path: String): List<String>? {
        var reader: BufferedReader? = null
        return try {
            reader = BufferedReader(FileReader(File(path)))
            reader.readLines()
        } catch (_: Exception) {
            null
        } finally {
            try { reader?.close() } catch (_: Exception) { }
        }
    }

    fun listChildren(path: String): List<File> {
        return try {
            File(path).listFiles()?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun exists(path: String): Boolean {
        return try { File(path).exists() } catch (_: Exception) { false }
    }

    /**
     * Doc /proc/stat.
     *
     * Luu y: tu Android 8 (API 26) tro di, ung dung thuong khong doc duoc
     * /proc/stat cua he thong (bi gioi han boi SELinux / hidepid). Ham nay chi
     * dung de tham khao khi he thong cho phep; neu tra null thi phia goi phai
     * chuyen sang cach uoc tinh khac (tan so + thoi gian CPU cua chinh tien
     * trinh) va ghi ro phuong phap trong snapshot. Khong bao gio suy dien
     * ti le tai he thong tu gia tri rong.
     */
    fun readProcStatCpuLine(): ProcStatCpu? {
        val lines = readAllLines("/proc/stat") ?: return null
        val cpuLine = lines.firstOrNull { it.startsWith("cpu ") } ?: return null
        val parts = cpuLine.trim().split(Regex("\\s+"))
        if (parts.size < 8) return null
        val values = parts.drop(1).mapNotNull { it.toLongOrNull() }
        if (values.size < 7) return null
        val idle = values[3] + values[4]
        val total = values.sum()
        return ProcStatCpu(idle = idle, total = total)
    }

    data class ProcStatCpu(val idle: Long, val total: Long) {
        fun loadSince(previous: ProcStatCpu): Double? {
            val totalDelta = total - previous.total
            val idleDelta = idle - previous.idle
            if (totalDelta <= 0) return null
            return ((totalDelta - idleDelta).toDouble() / totalDelta.toDouble()) * 100.0
        }
    }

    /**
     * Parse mot dong thermal zone: gia tri trong /sys/class/thermal/thermal_zoneN/temp
     * thuong la mili-do C (42000 = 42.0 do C). Mot so thiet bi tra thang do C.
     * Ham nay chuan hoa ve do C; neu gia tri vo ly (> 200 do C sau khi chia)
     * thi tra null.
     */
    fun parseThermalTempMilliC(raw: Long?): Double? {
        if (raw == null) return null
        val celsius = if (raw > 1000) raw / 1000.0 else raw.toDouble()
        if (celsius < -40.0 || celsius > 200.0) return null
        return celsius
    }

    /** Parse tan so CPU trong /sys/.../scaling_cur_freq, don vi kHz -> MHz. */
    fun parseFreqKhzToMhz(raw: Long?): Double? {
        if (raw == null || raw <= 0) return null
        return raw / 1000.0
    }
}
