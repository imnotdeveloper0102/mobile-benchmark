package com.mdevice.benchmark

import com.mdevice.benchmark.benchmark.ScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreCalculatorTest {

    @Test
    fun cpuScore_atReferenceTimes_isNear1000() {
        val score = ScoreCalculator.cpuScore(
            singleMs = ScoreCalculator.REF_CPU_SINGLE_MS,
            multiMs = ScoreCalculator.REF_CPU_MULTI_MS,
            sha256Ms = ScoreCalculator.REF_SHA256_MS,
            gzipMs = ScoreCalculator.REF_GZIP_MS,
            matrixMs = ScoreCalculator.REF_MATRIX_MS,
            sortMs = ScoreCalculator.REF_SORT_MS,
        )
        assertEquals(1000.0, score, 1.0)
    }

    @Test
    fun cpuScore_twiceAsSlow_isNear500() {
        val score = ScoreCalculator.cpuScore(
            singleMs = ScoreCalculator.REF_CPU_SINGLE_MS * 2,
            multiMs = ScoreCalculator.REF_CPU_MULTI_MS * 2,
            sha256Ms = ScoreCalculator.REF_SHA256_MS * 2,
            gzipMs = ScoreCalculator.REF_GZIP_MS * 2,
            matrixMs = ScoreCalculator.REF_MATRIX_MS * 2,
            sortMs = ScoreCalculator.REF_SORT_MS * 2,
        )
        assertEquals(500.0, score, 1.0)
    }

    @Test
    fun cpuScore_zeroTime_isZero() {
        val score = ScoreCalculator.cpuScore(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        assertEquals(0.0, score, 0.0)
    }

    @Test
    fun memScore_atReference_is1000() {
        val score = ScoreCalculator.memScore(
            readMBps = ScoreCalculator.REF_MEM_MBPS,
            writeMBps = ScoreCalculator.REF_MEM_MBPS,
            randomLatencyNs = 100.0,
        )
        assertEquals(1000.0, score, 1.0)
    }

    @Test
    fun storageScore_atReference_is1000() {
        val score = ScoreCalculator.storageScore(
            readMBps = ScoreCalculator.REF_STORAGE_MBPS,
            writeMBps = ScoreCalculator.REF_STORAGE_MBPS,
        )
        assertEquals(1000.0, score, 1.0)
    }

    @Test
    fun gpuScore_60fpsNoJank_is1000() {
        val score = ScoreCalculator.gpuScore(fps = 60.0, jankPct = 0.0)
        assertEquals(1000.0, score, 1.0)
    }

    @Test
    fun gpuScore_zeroFps_isZero() {
        assertEquals(0.0, ScoreCalculator.gpuScore(fps = 0.0, jankPct = 0.0), 0.0)
    }

    @Test
    fun totalScore_weightsSumToOne() {
        val scores = ScoreCalculator.calculate(
            cpuSingleMs = ScoreCalculator.REF_CPU_SINGLE_MS,
            cpuMultiMs = ScoreCalculator.REF_CPU_MULTI_MS,
            sha256Ms = ScoreCalculator.REF_SHA256_MS,
            gzipMs = ScoreCalculator.REF_GZIP_MS,
            matrixMs = ScoreCalculator.REF_MATRIX_MS,
            sortMs = ScoreCalculator.REF_SORT_MS,
            memReadMBps = ScoreCalculator.REF_MEM_MBPS,
            memWriteMBps = ScoreCalculator.REF_MEM_MBPS,
            randomLatencyNs = 100.0,
            storageReadMBps = ScoreCalculator.REF_STORAGE_MBPS,
            storageWriteMBps = ScoreCalculator.REF_STORAGE_MBPS,
            gpuFps = 60.0,
            gpuJankPct = 0.0,
        )
        assertEquals(1000.0, scores.totalScore, 1.0)
        assertTrue(scores.cpuScore in 0.0..1000.0)
        assertTrue(scores.memScore in 0.0..1000.0)
        assertTrue(scores.storageScore in 0.0..1000.0)
        assertTrue(scores.gpuScore in 0.0..1000.0)
    }

    @Test
    fun stressDropPct_20PercentDrop() {
        val drop = ScoreCalculator.stressDropPct(firstScore = 800.0, lastScore = 640.0)
        assertEquals(20.0, drop ?: -1.0, 0.001)
    }

    @Test
    fun stressDropPct_zeroFirst_isNull() {
        assertEquals(null, ScoreCalculator.stressDropPct(firstScore = 0.0, lastScore = 100.0))
    }
}
