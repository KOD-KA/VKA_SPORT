package com.vkasport.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vkasport.app.data.local.entity.CompletedWorkoutExerciseEntity

@Dao
interface CompletedWorkoutExerciseDao {

    @Insert
    suspend fun insert(
        exercise: CompletedWorkoutExerciseEntity
    )

    @Query(
        "SELECT * FROM completed_workout_exercises WHERE workoutId = :workoutId"
    )
    suspend fun getByWorkout(
        workoutId: Long
    ): List<CompletedWorkoutExerciseEntity>
}