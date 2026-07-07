package com.vkasport.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vkasport.app.data.local.entity.CompletedWorkoutSetEntity

@Dao
interface CompletedWorkoutSetDao {

    @Insert
    suspend fun insert(
        set: CompletedWorkoutSetEntity
    )

    @Query(
        "SELECT * FROM completed_workout_sets WHERE workoutId = :workoutId"
    )
    suspend fun getByWorkout(
        workoutId: Long
    ): List<CompletedWorkoutSetEntity>

    @Query("DELETE FROM completed_workout_sets WHERE workoutId = :workoutId")
    suspend fun deleteByWorkout(workoutId: Long)
}