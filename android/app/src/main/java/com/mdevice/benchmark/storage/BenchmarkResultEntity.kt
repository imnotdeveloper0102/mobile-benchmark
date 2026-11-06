package com.mdevice.benchmark.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmark_results")
data class BenchmarkResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMs: Long,
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
    val cpuScore: Double,
    val memScore: Double,
    val storageScore: Double,
    val gpuScore: Double,
    val totalScore: Double,
    val stressDropPct: Double?,
    val deviceModel: String,
)
