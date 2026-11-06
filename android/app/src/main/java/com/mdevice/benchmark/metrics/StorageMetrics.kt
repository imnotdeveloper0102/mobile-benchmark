package com.mdevice.benchmark.metrics

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.random.Random

/**
 * Dung luong + toc do doc/ghi that.
 *
 * Toc do do bang file tam trong cacheDir cua app (khong dung bo nho ngoai),
 * ghi tuan tu 32MB, doc lai tuan tu, doc/ghi ngau nhien 4K. Sau khi do xong
 * file tam duoc xoa ngay trong finally. Moi phep do dung System.nanoTime.
 * Ket qua phu thuoc vao trang thai bo nho luc do (cache, nen) nen chi mang
 * tinh tham khao tai thoi diem do.
 */
class StorageMetrics(private val context: Context) {

    data class Capacity(
        val internalTotalBytes: Long,
        val internalAvailBytes: Long,
        val externalTotalBytes: Long?,
    )

    data class SpeedResult(
        val seqWriteMBps: Double,
        val seqReadMBps: Double,
        val randomReadIOPS: Double,
        val randomWriteIOPS: Double,
        val fileSizeMB: Int,
    )

    fun capacity(): Capacity {
        val internal = StatFs(Environment.getDataDirectory().absolutePath)
        val internalTotal = internal.blockCountLong * internal.blockSizeLong
        val internalAvail = internal.availableBlocksLong * internal.blockSizeLong
        val externalTotal: Long? = try {
            val ext = context.getExternalFilesDir(null)
            if (ext != null) {
                val stat = StatFs(ext.absolutePath)
                stat.blockCountLong * stat.blockSizeLong
            } else null
        } catch (_: Exception) {
            null
        }
        return Capacity(internalTotal, internalAvail, externalTotal)
    }

    fun measureSpeed(fileSizeMB: Int = 32): SpeedResult? {
        val dir = context.cacheDir ?: return null
        val file = File(dir, "bench_speed_${System.nanoTime()}.tmp")
        val sizeBytes = fileSizeMB * 1024 * 1024
        val buffer = ByteArray(1024 * 1024)
        Random.nextBytes(buffer)
        var output: FileOutputStream? = null
        var input: FileInputStream? = null
        var raf: RandomAccessFile? = null
        return try {
            val writeStart = System.nanoTime()
            output = FileOutputStream(file)
            var written = 0
            while (written < sizeBytes) {
                output.write(buffer)
                written += buffer.size
            }
            output.fd.sync()
            val writeMs = (System.nanoTime() - writeStart) / 1_000_000.0
            output.close()
            output = null

            val readStart = System.nanoTime()
            input = FileInputStream(file)
            val readBuf = ByteArray(1024 * 1024)
            var read: Int
            var totalRead = 0
            while (input.read(readBuf).also { read = it } != -1) {
                totalRead += read
            }
            val readMs = (System.nanoTime() - readStart) / 1_000_000.0
            input.close()
            input = null

            // Ngau nhien 4K: 512 lan doc + 512 lan ghi tai vi tri ngau nhien.
            val ops = 512
            val small = ByteArray(4096)
            Random.nextBytes(small)
            raf = RandomAccessFile(file, "rw")
            val randReadStart = System.nanoTime()
            repeat(ops) {
                val pos = Random.nextLong(0, (sizeBytes - 4096).toLong())
                raf.seek(pos)
                raf.readFully(small)
            }
            val randReadMs = (System.nanoTime() - randReadStart) / 1_000_000.0
            val randWriteStart = System.nanoTime()
            repeat(ops) {
                val pos = Random.nextLong(0, (sizeBytes - 4096).toLong())
                raf.seek(pos)
                raf.write(small)
            }
            val randWriteMs = (System.nanoTime() - randWriteStart) / 1_000_000.0

            SpeedResult(
                seqWriteMBps = if (writeMs > 0) fileSizeMB.toDouble() / (writeMs / 1000.0) else 0.0,
                seqReadMBps = if (readMs > 0) fileSizeMB.toDouble() / (readMs / 1000.0) else 0.0,
                randomReadIOPS = if (randReadMs > 0) ops / (randReadMs / 1000.0) else 0.0,
                randomWriteIOPS = if (randWriteMs > 0) ops / (randWriteMs / 1000.0) else 0.0,
                fileSizeMB = fileSizeMB,
            )
        } catch (_: Exception) {
            null
        } finally {
            try { output?.close() } catch (_: Exception) { }
            try { input?.close() } catch (_: Exception) { }
            try { raf?.close() } catch (_: Exception) { }
            try { if (file.exists()) file.delete() } catch (_: Exception) { }
        }
    }
}
