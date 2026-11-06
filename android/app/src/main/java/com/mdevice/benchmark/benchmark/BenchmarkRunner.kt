package com.mdevice.benchmark.benchmark

import android.content.Context
import android.os.Build
import com.mdevice.benchmark.metrics.StorageMetrics
import com.mdevice.benchmark.metrics.ThermalMetrics
import com.mdevice.benchmark.metrics.CpuMetrics
import com.mdevice.benchmark.storage.AppDatabase
import com.mdevice.benchmark.storage.BenchmarkResultEntity
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Chay benchmark that tren nhieu luong (Dispatchers.Default).
 *
 * Tat ca cac bai deu chay that va do bang System.nanoTime, khong dung gia
 * tri co dinh. Ket qua duoc luu vao Room va tra ve dang JSON de phia JS
 * hien thi + so sanh. Stress test chay tai nang lien tuc theo
 * durationMin (mac dinh 5 phut), ghi nhiet + tan so moi 5 giay, tinh
 * % giam hieu nang = (diem dau - diem cuoi) / diem dau.
 */
class BenchmarkRunner(private val context: Context) {

    data class StressPoint(
        val atMs: Long,
        val batteryTempC: Double?,
        val cpuTempC: Double?,
        val freqAvgMHz: Double?,
        val roundScore: Double,
    )

    data class RunResult(
        val id: Long = 0L,
        val startedAtMs: Long = 0L,
        val endedAtMs: Long = 0L,
        val cpuSingleMs: Double,
        val cpuMultiMs: Double,
        val sha256Ms: Double,
        val gzipMs: Double,
        val matrixMs: Double,
        val sortMs: Double,
        val memReadMBps: Double,
        val memWriteMBps: Double,
        val randomLatencyNs: Double,
        val storageReadMBps: Double,
        val storageWriteMBps: Double,
        val gpuFps: Double,
        val gpuJankPct: Double,
        val scores: ScoreCalculator.Scores,
        val stressDropPct: Double?,
        val stressPoints: List<StressPoint>,
    )

    fun primeSieveCount(limit: Int): Int {
        if (limit < 2) return 0
        val isPrime = BooleanArray(limit + 1) { true }
        isPrime[0] = false
        isPrime[1] = false
        val root = sqrt(limit.toDouble()).toInt()
        for (i in 2..root) {
            if (isPrime[i]) {
                var j = i * i
                while (j <= limit) {
                    isPrime[j] = false
                    j += i
                }
            }
        }
        return isPrime.count { it }
    }

    suspend fun cpuSingle(): Double = withContext(Dispatchers.Default) {
        val start = System.nanoTime()
        primeSieveCount(2_000_000)
        (System.nanoTime() - start) / 1_000_000.0
    }

    suspend fun cpuMulti(): Double = withContext(Dispatchers.Default) {
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val start = System.nanoTime()
        coroutineScope {
            val jobs = (0 until cores).map {
                async { primeSieveCount(2_000_000) }
            }
            jobs.awaitAll()
        }
        (System.nanoTime() - start) / 1_000_000.0
    }

    suspend fun sha256(): Double = withContext(Dispatchers.Default) {
        val data = ByteArray(64 * 1024 * 1024)
        Random.nextBytes(data)
        val start = System.nanoTime()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(data)
        digest.digest()
        (System.nanoTime() - start) / 1_000_000.0
    }

    suspend fun gzip(): Double = withContext(Dispatchers.Default) {
        val data = ByteArray(32 * 1024 * 1024)
        Random.nextBytes(data)
        val start = System.nanoTime()
        val out = ByteArrayOutputStream()
        var gzip: GZIPOutputStream? = null
        try {
            gzip = GZIPOutputStream(out)
            gzip.write(data)
            gzip.finish()
        } finally {
            try { gzip?.close() } catch (_: Exception) { }
            try { out.close() } catch (_: Exception) { }
        }
        (System.nanoTime() - start) / 1_000_000.0
    }

    suspend fun matrixMultiply(): Double = withContext(Dispatchers.Default) {
        val n = 512
        val a = FloatArray(n * n) { Random.nextFloat() }
        val b = FloatArray(n * n) { Random.nextFloat() }
        val c = FloatArray(n * n)
        val start = System.nanoTime()
        for (i in 0 until n) {
            for (k in 0 until n) {
                val aik = a[i * n + k]
                for (j in 0 until n) {
                    c[i * n + j] += aik * b[k * n + j]
                }
            }
        }
        // Doc ket qua de trinh bien dich khong bo vong lap.
        var checksum = 0f
        for (v in c) checksum += v
        if (checksum == Float.MIN_VALUE) throw IllegalStateException("checksum khong the xay ra")
        (System.nanoTime() - start) / 1_000_000.0
    }

    suspend fun sortLarge(): Double = withContext(Dispatchers.Default) {
        val array = IntArray(1_000_000) { Random.nextInt() }
        val start = System.nanoTime()
        array.sort()
        (System.nanoTime() - start) / 1_000_000.0
    }

    suspend fun memoryBandwidth(): Triple<Double, Double, Double> = withContext(Dispatchers.Default) {
        val size = 64 * 1024 * 1024
        val src = ByteArray(size)
        val dst = ByteArray(size)
        Random.nextBytes(src)
        val writeStart = System.nanoTime()
        src.copyInto(dst)
        val writeMs = (System.nanoTime() - writeStart) / 1_000_000.0
        val readStart = System.nanoTime()
        var sum = 0L
        for (i in dst.indices step 4096) sum += dst[i]
        val readMs = (System.nanoTime() - readStart) / 1_000_000.0
        if (sum == Long.MIN_VALUE) throw IllegalStateException("tong khong the xay ra")
        // Do tre truy cap ngau nhien: 10000 lan doc ngau nhien 8 byte.
        val intArray = IntArray(8 * 1024 * 1024) { it }
        val latStart = System.nanoTime()
        var acc = 0
        repeat(10000) {
            acc += intArray[Random.nextInt(intArray.size)]
        }
        val latNs = (System.nanoTime() - latStart) / 10000.0
        if (acc == Int.MIN_VALUE) throw IllegalStateException("tong khong the xay ra")
        val writeMBps = if (writeMs > 0) size / 1024.0 / 1024.0 / (writeMs / 1000.0) else 0.0
        val readMBps = if (readMs > 0) size / 1024.0 / 1024.0 / (readMs / 1000.0) else 0.0
        Triple(readMBps, writeMBps, latNs)
    }

    /**
     * Chay don nhan: chi prime don + SHA-256 + sort. Cac bai khong chay
     * de 0, diem mem/storage/gpu = 0, diem tong chi con phan CPU
     * (0.4 * cpuScore) nen KHONG so sanh duoc voi lan chay full.
     */
    suspend fun runSingle(): RunResult {
        val startedAtMs = System.currentTimeMillis()
        val single = cpuSingle()
        val sha = sha256()
        val sort = sortLarge()
        val cpu = ScoreCalculator.cpuScoreSingle(single, sha, sort)
        val scores = ScoreCalculator.Scores(cpu, 0.0, 0.0, 0.0, 0.0)
            .copy(totalScore = ScoreCalculator.total(ScoreCalculator.Scores(cpu, 0.0, 0.0, 0.0, 0.0)))
        val result = RunResult(
            startedAtMs = startedAtMs, endedAtMs = System.currentTimeMillis(),
            cpuSingleMs = single, cpuMultiMs = 0.0, sha256Ms = sha,
            gzipMs = 0.0, matrixMs = 0.0, sortMs = sort,
            memReadMBps = 0.0, memWriteMBps = 0.0, randomLatencyNs = 0.0,
            storageReadMBps = 0.0, storageWriteMBps = 0.0,
            gpuFps = 0.0, gpuJankPct = 0.0,
            scores = scores, stressDropPct = null, stressPoints = emptyList(),
        )
        return result.copy(id = save(result))
    }

    /**
     * Chay da nhan: chi prime da + nhan ma tran + sort.
     * Giong runSingle: cac thanh phan khong chay = 0.
     */
    suspend fun runMulti(): RunResult {
        val startedAtMs = System.currentTimeMillis()
        val multi = cpuMulti()
        val matrix = matrixMultiply()
        val sort = sortLarge()
        val cpu = ScoreCalculator.cpuScoreMulti(multi, matrix, sort)
        val partial = ScoreCalculator.Scores(cpu, 0.0, 0.0, 0.0, 0.0)
        val scores = partial.copy(totalScore = ScoreCalculator.total(partial))
        val result = RunResult(
            startedAtMs = startedAtMs, endedAtMs = System.currentTimeMillis(),
            cpuSingleMs = 0.0, cpuMultiMs = multi, sha256Ms = 0.0,
            gzipMs = 0.0, matrixMs = matrix, sortMs = sort,
            memReadMBps = 0.0, memWriteMBps = 0.0, randomLatencyNs = 0.0,
            storageReadMBps = 0.0, storageWriteMBps = 0.0,
            gpuFps = 0.0, gpuJankPct = 0.0,
            scores = scores, stressDropPct = null, stressPoints = emptyList(),
        )
        return result.copy(id = save(result))
    }

    /**
     * Chay tron bo benchmark. GPU chua do duoc qua bridge nay
     * (can GLSurfaceView tren UI thread, chua gan GpuBenchmarkView),
     * nen gpuFps = 0 va diem GPU = 0. Khong bia FPS GPU.
     */
    suspend fun runFull(): RunResult {
        val startedAtMs = System.currentTimeMillis()
        val single = cpuSingle()
        val multi = cpuMulti()
        val sha = sha256()
        val gz = gzip()
        val matrix = matrixMultiply()
        val sort = sortLarge()
        val (memRead, memWrite, latency) = memoryBandwidth()
        val storage = StorageMetrics(context).measureSpeed(32)
        val storageRead = storage?.seqReadMBps ?: 0.0
        val storageWrite = storage?.seqWriteMBps ?: 0.0
        val scores = ScoreCalculator.calculate(
            cpuSingleMs = single, cpuMultiMs = multi, sha256Ms = sha,
            gzipMs = gz, matrixMs = matrix, sortMs = sort,
            memReadMBps = memRead, memWriteMBps = memWrite,
            randomLatencyNs = latency,
            storageReadMBps = storageRead, storageWriteMBps = storageWrite,
            gpuFps = 0.0, gpuJankPct = 0.0,
        )
        val result = RunResult(
            startedAtMs = startedAtMs, endedAtMs = System.currentTimeMillis(),
            cpuSingleMs = single, cpuMultiMs = multi, sha256Ms = sha,
            gzipMs = gz, matrixMs = matrix, sortMs = sort,
            memReadMBps = memRead, memWriteMBps = memWrite,
            randomLatencyNs = latency,
            storageReadMBps = storageRead, storageWriteMBps = storageWrite,
            gpuFps = 0.0, gpuJankPct = 0.0,
            scores = scores, stressDropPct = null, stressPoints = emptyList(),
        )
        return result.copy(id = save(result))
    }

    /** Dieu huong theo mode ma phia JS goi: single | multi | full | stress. */
    suspend fun runByMode(mode: String): RunResult {
        return when (mode.trim().lowercase()) {
            "single" -> runSingle()
            "multi" -> runMulti()
            "stress" -> runStress(5)
            "full" -> runFull()
            else -> throw IllegalArgumentException("Mode benchmark khong hop le: $mode")
        }
    }

    /**
     * Stress test: lap lai bai prime da nhan trong durationMin phut, moi 5
     * giay ghi 1 diem (nhiet + tan so + diem vong). Diem vong la diem CPU
     * cua vong prime do, dung de tinh % giam hieu nang giua vong dau va
     * vong cuoi (throttling).
     */
    suspend fun runStress(durationMin: Int = 5): RunResult {
        val thermal = ThermalMetrics(context)
        val cpu = CpuMetrics()
        val durationMs = durationMin.coerceIn(1, 10) * 60_000L
        val points = mutableListOf<StressPoint>()
        val startMs = System.currentTimeMillis()
        var firstScore: Double? = null
        var lastScore: Double? = null
        val base = runFull()
        firstScore = base.scores.cpuScore
        lastScore = firstScore
        var elapsed = 0L
        while (elapsed < durationMs) {
            val roundMs = cpuMulti()
            val roundScore = ScoreCalculator.timeScoreForStress(roundMs)
            lastScore = roundScore
            val temps = thermal.collect()
            val freqs = cpu.currentFrequencies(cpu.coreCount())
            val avgFreq = freqs.filterNotNull().average().takeIf { !it.isNaN() }
            points.add(
                StressPoint(
                    atMs = System.currentTimeMillis() - startMs,
                    batteryTempC = temps.batteryTempC,
                    cpuTempC = temps.cpuTempC,
                    freqAvgMHz = avgFreq,
                    roundScore = roundScore,
                ),
            )
            elapsed = System.currentTimeMillis() - startMs
            val remaining = durationMs - elapsed
            if (remaining > 0) delay(minOf(5000L, remaining))
        }
        val drop = ScoreCalculator.stressDropPct(firstScore ?: 0.0, lastScore ?: 0.0)
        val stressed = base.copy(
            endedAtMs = System.currentTimeMillis(),
            stressDropPct = drop, stressPoints = points,
        )
        // runFull da luu 1 dong cho base; stress luu them 1 dong moi co drop.
        return stressed.copy(id = save(stressed))
    }

    private suspend fun save(result: RunResult): Long {
        return try {
            val db = AppDatabase.get(context)
            db.benchmarkDao().insert(
                BenchmarkResultEntity(
                    timestampMs = result.startedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    cpuSingleMs = result.cpuSingleMs,
                    cpuMultiMs = result.cpuMultiMs,
                    sha256Ms = result.sha256Ms,
                    gzipMs = result.gzipMs,
                    matrixMs = result.matrixMs,
                    sortMs = result.sortMs,
                    memReadMBps = result.memReadMBps,
                    memWriteMBps = result.memWriteMBps,
                    randomLatencyNs = result.randomLatencyNs,
                    storageReadMBps = result.storageReadMBps,
                    storageWriteMBps = result.storageWriteMBps,
                    gpuFps = result.gpuFps,
                    gpuJankPct = result.gpuJankPct,
                    cpuScore = result.scores.cpuScore,
                    memScore = result.scores.memScore,
                    storageScore = result.scores.storageScore,
                    gpuScore = result.scores.gpuScore,
                    totalScore = result.scores.totalScore,
                    stressDropPct = result.stressDropPct,
                    deviceModel = Build.MODEL ?: "Khong ro",
                ),
            )
        } catch (_: Exception) {
            // Luu lich su that bai khong duoc lam hong ket qua benchmark vua chay.
            0L
        }
    }

    fun toJson(result: RunResult): JSONObject {
        val stressArray = org.json.JSONArray()
        for (p in result.stressPoints) {
            stressArray.put(
                JSONObject().apply {
                    put("atMs", p.atMs)
                    if (p.batteryTempC != null) put("batteryTempC", p.batteryTempC)
                    if (p.cpuTempC != null) put("cpuTempC", p.cpuTempC)
                    if (p.freqAvgMHz != null) put("freqAvgMHz", p.freqAvgMHz)
                    put("roundScore", p.roundScore)
                },
            )
        }
        // Khop NativeBenchmarkResult ben TS: id dang string, scores la
        // object {cpu, memory, storage, gpu, total}, co startedAtMs /
        // endedAtMs / deviceModel. stressDropPct = null khi khong phai
        // stress test, phia JS hien "Khong kha dung" cho truong nay.
        return JSONObject().apply {
            put("id", result.id.toString())
            put("startedAtMs", result.startedAtMs)
            put("endedAtMs", result.endedAtMs)
            put("cpuSingleMs", result.cpuSingleMs)
            put("cpuMultiMs", result.cpuMultiMs)
            put("sha256Ms", result.sha256Ms)
            put("gzipMs", result.gzipMs)
            put("matrixMs", result.matrixMs)
            // Ben TS goi truong nay la matmulMs (xem NativeBenchmarkResult).
            put("matmulMs", result.matrixMs)
            put("sortMs", result.sortMs)
            put("memReadMBps", result.memReadMBps)
            put("memWriteMBps", result.memWriteMBps)
            put("randomLatencyNs", result.randomLatencyNs)
            put("memRandomLatencyNs", result.randomLatencyNs)
            put("storageReadMBps", result.storageReadMBps)
            put("storageWriteMBps", result.storageWriteMBps)
            put("gpuFps", result.gpuFps)
            put("gpuJankPct", result.gpuJankPct)
            if (result.stressDropPct != null) put("stressDropPct", result.stressDropPct)
            else put("stressDropPct", JSONObject.NULL)
            put("scores", JSONObject().apply {
                put("cpu", result.scores.cpuScore)
                put("memory", result.scores.memScore)
                put("storage", result.scores.storageScore)
                put("gpu", result.scores.gpuScore)
                put("total", result.scores.totalScore)
            })
            put("deviceModel", Build.MODEL ?: "Khong ro")
            put("stressPoints", stressArray)
        }
    }
}
