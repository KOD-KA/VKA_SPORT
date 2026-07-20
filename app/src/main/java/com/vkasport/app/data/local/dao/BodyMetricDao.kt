package com.vkasport.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vkasport.app.data.local.entity.BodyMetricEntity

@Dao
interface BodyMetricDao {

    @Insert
    suspend fun insert(entity: BodyMetricEntity): Long

    @Query("SELECT * FROM body_metrics ORDER BY date ASC")
    suspend fun getAll(): List<BodyMetricEntity>

    @Query("DELETE FROM body_metrics WHERE id = :id")
    suspend fun deleteById(id: Long)
}