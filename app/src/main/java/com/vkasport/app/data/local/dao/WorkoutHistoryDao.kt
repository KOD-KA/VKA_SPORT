package com.vkasport.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vkasport.app.data.local.entity.CompletedWorkoutEntity

@Dao
interface WorkoutHistoryDao {

    @Insert
    suspend fun insertWorkout(
        workout: CompletedWorkoutEntity
    ): Long

    @Query(
        "SELECT * FROM completed_workouts ORDER BY date DESC"
    )
    suspend fun getAllWorkouts():
            List<CompletedWorkoutEntity>

    @Query("UPDATE completed_workouts SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String?)

    @Query("DELETE FROM completed_workouts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "DELETE FROM completed_workouts"
    )
    suspend fun clear()
}