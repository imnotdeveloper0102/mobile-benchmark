package com.mdevice.benchmark.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MetricDao {
    @Insert
    suspend fun insert(metric: MetricEntity): Long

    @Insert
    suspend fun insertAll(metrics: List<MetricEntity>)

    @Query("SELECT * FROM metrics WHERE timestampMs BETWEEN :fromMs AND :toMs ORDER BY timestampMs ASC")
    suspend fun getBetween(fromMs: Long, toMs: Long): List<MetricEntity>

    @Query("SELECT * FROM metrics ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<MetricEntity>

    @Query("SELECT COUNT(*) FROM metrics")
    suspend fun count(): Int

    @Query("DELETE FROM metrics WHERE timestampMs < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long): Int

    @Query("SELECT MAX(batteryTempC) FROM metrics WHERE timestampMs BETWEEN :fromMs AND :toMs")
    suspend fun maxBatteryTempBetween(fromMs: Long, toMs: Long): Double?
}
