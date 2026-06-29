package com.vkasport.app.data.model

import java.time.LocalDate

data class PlannedWorkout(
    val id: Long = 0,
    val date: LocalDate,
    val hour: Int,
    val minute: Int,
    val muscleGroup: String,
    val exercises: List<String> = emptyList()
)