package com.vkasport.app.data.model

import java.time.LocalDateTime

data class CompletedWorkout(

    val id: Long = 0,

    val dateTime: LocalDateTime,

    val athleteWeight: Float?,

    val muscleGroup: String,

    val exercises: List<WorkoutExercise>,

    val durationMinutes: Long = 0,

    val notes: String? = null
)