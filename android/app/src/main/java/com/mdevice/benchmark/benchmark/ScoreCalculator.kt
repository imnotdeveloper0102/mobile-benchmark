package com.mdevice.benchmark.benchmark

import kotlin.math.min

/**
 * Cong thuc tinh diem - ghi trong README cua project.
 *
 * Moi thanh phan duoc chuan hoa ve thang 0..1000 roi cong co trong so:
 *   total = 0.4 * cpu + 0.2 * mem + 0.2 * storage + 0.2 * gpu
 *
 * - cpuScore = 1000 * (refTimeMs / timeMs), voi refTimeMs la thoi gian chuan
 *   do tren may tham chieu (xem cac hang REF_* ben duoi). timeMs la trung
 *   binh co trong so cua cac bai CPU: prime don nhan, prime da nhan,
 *   SHA-256, GZIP, nhan ma tran, sap xep. Gia tri bi clamp 0..1000.
 * - memScore = 1000 * (bandwidthMBps / REF_MEM_MBPS), clamp 0..1000.
 *   bandwidth = trung binh doc + ghi mang 64MB.
 * - storageScore = 1000 * (readMBps + writeMBps) / (2 * REF_STORAGE_MBPS),
 *   clamp 0..1000.
 * - gpuScore = 1000 * (fps / REF_GPU_FPS) * (1 - jankPct/200),
 *   clamp 0..1000. He so jank giam diem khi khung rot nhieu.
 *
 * Cac hang REF_* la gia tri tham chieu co dinh de diem so sanh duoc giua
 * cac lan chay tren cung mot may va giua cac may voi nhau. Chung khong phai
 * la gioi han phan cung that, chi la moc chuan hoa.
 */
object ScoreCalculator {

    const val REF_CPU_SINGLE_MS = 1200.0
    const val REF_CPU_MULTI_MS = 350.0
    const val REF_SHA256_MS = 900.0
    const val REF_GZIP_MS = 1100.0
    const val REF_MATRIX_MS = 800.0
    const val REF_SORT_MS = 450.0
    const val REF_MEM_MBPS = 12000.0
    const val REF_STORAGE_MBPS = 800.0
    const val REF_GPU_FPS = 60.0

    const val WEIGHT_CPU = 0.4
    const val WEIGHT_MEM = 0.2
    const val WEIGHT_STORAGE = 0.2
    const val WEIGHT_GPU = 0.2

    data class Scores(
        val cpuScore: Double,
        val memScore: Double,
        val storageScore: Double,
        val gpuScore: Double,
        val totalScore: Double,
    )

    private fun clampScore(value: Double): Double = value.coerceIn(0.0, 1000.0)

    private fun timeScore(refMs: Double, actualMs: Double): Double {
        if (actualMs <= 0.0) return 0.0
        return clampScore(1000.0 * (refMs / actualMs))
    }

    fun cpuScore(
        singleMs: Double,
        multiMs: Double,
        sha256Ms: Double,
        gzipMs: Double,
        matrixMs: Double,
        sortMs: Double,
    ): Double {
        // Trong so noi bo bai CPU: prime don 0.25, prime da 0.25, SHA 0.15,
        // GZIP 0.10, ma tran 0.15, sap xep 0.10.
        val weighted = 0.25 * timeScore(REF_CPU_SINGLE_MS, singleMs) +
            0.25 * timeScore(REF_CPU_MULTI_MS, multiMs) +
            0.15 * timeScore(REF_SHA256_MS, sha256Ms) +
            0.10 * timeScore(REF_GZIP_MS, gzipMs) +
            0.15 * timeScore(REF_MATRIX_MS, matrixMs) +
            0.10 * timeScore(REF_SORT_MS, sortMs)
        return clampScore(weighted)
    }

    /**
     * Diem CPU cho chay don nhan (chi prime don + SHA-256 + sort).
     * Trong so goc 0.25/0.15/0.10 duoc chuan hoa lai ve tong 1 (0.5/0.3/0.2)
     * vi cac bai CPU con lai khong chay o mode nay.
     */
    fun cpuScoreSingle(singleMs: Double, sha256Ms: Double, sortMs: Double): Double {
        val weighted = 0.5 * timeScore(REF_CPU_SINGLE_MS, singleMs) +
            0.3 * timeScore(REF_SHA256_MS, sha256Ms) +
            0.2 * timeScore(REF_SORT_MS, sortMs)
        return clampScore(weighted)
    }

    /**
     * Diem CPU cho chay da nhan (chi prime da + nhan ma tran + sort),
     * chuan hoa tuong tu: 0.25/0.15/0.10 -> 0.5/0.3/0.2.
     */
    fun cpuScoreMulti(multiMs: Double, matrixMs: Double, sortMs: Double): Double {
        val weighted = 0.5 * timeScore(REF_CPU_MULTI_MS, multiMs) +
            0.3 * timeScore(REF_MATRIX_MS, matrixMs) +
            0.2 * timeScore(REF_SORT_MS, sortMs)
        return clampScore(weighted)
    }

    fun memScore(readMBps: Double, writeMBps: Double, randomLatencyNs: Double): Double {
        if (readMBps <= 0.0 && writeMBps <= 0.0) return 0.0
        val bandwidth = (readMBps + writeMBps) / 2.0
        var score = 1000.0 * (bandwidth / REF_MEM_MBPS)
        // Do tre ngau nhien cao thi tru toi da 20%: moc 300ns la tot.
        if (randomLatencyNs > 300.0) {
            val penalty = min(0.20, (randomLatencyNs - 300.0) / 3000.0)
            score *= (1.0 - penalty)
        }
        return clampScore(score)
    }

    fun storageScore(readMBps: Double, writeMBps: Double): Double {
        if (readMBps <= 0.0 && writeMBps <= 0.0) return 0.0
        val avg = (readMBps + writeMBps) / 2.0
        return clampScore(1000.0 * (avg / REF_STORAGE_MBPS))
    }

    fun gpuScore(fps: Double, jankPct: Double): Double {
        if (fps <= 0.0) return 0.0
        val jankFactor = (1.0 - jankPct.coerceIn(0.0, 100.0) / 200.0).coerceIn(0.5, 1.0)
        return clampScore(1000.0 * (fps / REF_GPU_FPS) * jankFactor)
    }

    fun total(scores: Scores): Double {
        return WEIGHT_CPU * scores.cpuScore +
            WEIGHT_MEM * scores.memScore +
            WEIGHT_STORAGE * scores.storageScore +
            WEIGHT_GPU * scores.gpuScore
    }

    fun calculate(
        cpuSingleMs: Double,
        cpuMultiMs: Double,
        sha256Ms: Double,
        gzipMs: Double,
        matrixMs: Double,
        sortMs: Double,
        memReadMBps: Double,
        memWriteMBps: Double,
        randomLatencyNs: Double,
        storageReadMBps: Double,
        storageWriteMBps: Double,
        gpuFps: Double,
        gpuJankPct: Double,
    ): Scores {
        val cpu = cpuScore(cpuSingleMs, cpuMultiMs, sha256Ms, gzipMs, matrixMs, sortMs)
        val mem = memScore(memReadMBps, memWriteMBps, randomLatencyNs)
        val storage = storageScore(storageReadMBps, storageWriteMBps)
        val gpu = gpuScore(gpuFps, gpuJankPct)
        val partial = Scores(cpu, mem, storage, gpu, 0.0)
        return partial.copy(totalScore = total(partial))
    }

    /** Diem CPU cua mot vong stress (chi bai prime da nhan), dung de so sanh vong dau / vong cuoi. */
    fun timeScoreForStress(multiMs: Double): Double {
        return timeScore(REF_CPU_MULTI_MS, multiMs)
    }

    /** % giam hieu nang trong stress test: (dau - cuoi) / dau * 100. */
    fun stressDropPct(firstScore: Double, lastScore: Double): Double? {
        if (firstScore <= 0.0) return null
        return ((firstScore - lastScore) / firstScore * 100.0).coerceIn(-100.0, 100.0)
    }
}
