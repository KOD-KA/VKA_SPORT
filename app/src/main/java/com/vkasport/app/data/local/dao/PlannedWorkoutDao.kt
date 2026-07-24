package com.vkasport.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.vkasport.app.data.local.entity.PlannedExerciseEntity
import com.vkasport.app.data.local.entity.PlannedWorkoutEntity

@Dao
interface PlannedWorkoutDao {

    // ── Тренировки ──────────────────────────────────────────────────
    @Insert
    suspend fun insert(entity: PlannedWorkoutEntity): Long

    @Query("SELECT * FROM planned_workouts ORDER BY date ASC, hour ASC, minute ASC")
    suspend fun getAll(): List<PlannedWorkoutEntity>

    @Query("DELETE FROM planned_workouts WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ── Упражнения ──────────────────────────────────────────────────
    @Insert
    suspend fun insertExercise(entity: PlannedExerciseEntity)

    @Query("SELECT * FROM planned_exercises WHERE workoutId = :workoutId")
    suspend fun getExercises(workoutId: Long): List<PlannedExerciseEntity>

    @Query("DELETE FROM planned_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesByWorkout(workoutId: Long)

    @Query("UPDATE planned_exercises SET exerciseName = :newName WHERE exerciseName = :oldName")
    suspend fun renameExercise(oldName: String, newName: String)
}