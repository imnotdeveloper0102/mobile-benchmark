package com.mdevice.benchmark.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BenchmarkDao {
    @Insert
    suspend fun insert(result: BenchmarkResultEntity): Long

    @Query("SELECT * FROM benchmark_results ORDER BY timestampMs DESC")
    suspend fun getAll(): List<BenchmarkResultEntity>

    @Query("SELECT * FROM benchmark_results ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<BenchmarkResultEntity>

    @Query("SELECT * FROM benchmark_results WHERE id = :id")
    suspend fun getById(id: Long): BenchmarkResultEntity?
}
