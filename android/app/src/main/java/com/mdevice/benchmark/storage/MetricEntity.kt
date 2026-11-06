package com.mdevice.benchmark.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mot mau da gop: 1 dong / 1 giay. Service chi ghi 1 lan/giay (khong ghi
 * tung mau 750ms) de co so luong dong vua phai; du lieu cu > 30 ngay duoc
 * xoa dinh ky o MetricDao / AlertWorker.
 */
@Entity(tableName = "metrics")
data class MetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMs: Long,
    val cpuLoadPct: Double?,
    val cpuFreqAvgMHz: Double?,
    val ramUsedBytes: Long,
    val ramTotalBytes: Long,
    val batteryTempC: Double?,
    val cpuTempC: Double?,
    val batteryLevelPct: Int,
    val charging: Boolean,
    val fps: Double,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
)
