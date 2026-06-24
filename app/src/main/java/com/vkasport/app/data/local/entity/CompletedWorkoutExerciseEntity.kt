package com.vkasport.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_workout_exercises")
data class CompletedWorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val workoutId: Long,

    val exerciseName: String
)