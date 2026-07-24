package com.vkasport.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vkasport.app.data.local.entity.CustomExerciseEntity

@Dao
interface CustomExerciseDao {

    @Insert
    suspend fun insert(entity: CustomExerciseEntity): Long

    @Query("SELECT * FROM custom_exercises ORDER BY name ASC")
    suspend fun getAll(): List<CustomExerciseEntity>

    @Query("UPDATE custom_exercises SET name = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: String)
}