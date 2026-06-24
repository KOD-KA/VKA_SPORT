package com.vkasport.app.data.model

import java.time.LocalDateTime

data class CurrentWorkoutState(

    val workoutStartTime: LocalDateTime =
        LocalDateTime.now(),

    val athleteWeight: Float? = null,

    val selectedMuscleGroup: MuscleGroup? = null,

    val selectedExercises: List<WorkoutExercise> = emptyList(),

    val trainingStarted: Boolean = false,

    val currentScreen: String = "start"

)