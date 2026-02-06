package com.example.toxiguard.data.db

import androidx.room.*

@Dao
interface DetectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetection(d: Detection): Long

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    suspend fun getAllDetections(): List<Detection>

    @Query("SELECT * FROM detections WHERE confirmedByML = 0 ORDER BY timestamp DESC")
    suspend fun getPendingMlConfirmations(): List<Detection>

    @Query("SELECT * FROM detections ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentDetectionsLimit(limit: Int): List<Detection>

    @Query("DELETE FROM detections WHERE timestamp < :ts")
    suspend fun deleteOlderThan(ts: Long)

    @Query("DELETE FROM detections")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    fun recentDetections(): kotlinx.coroutines.flow.Flow<List<Detection>>

    @Query("UPDATE detections SET confirmedByML = :confirmedByML, mlScore = :mlScore, isHiddenToxic = :isHidden WHERE id = :id")
    suspend fun updateConfirmation(id: Long, confirmedByML: Boolean, mlScore: Float?, isHidden: Boolean = false)
}
