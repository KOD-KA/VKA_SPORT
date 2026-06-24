package com.vkasport.app.data.model

data class WorkoutExercise(

    val name: String,

    val sets: List<WorkoutSet> = emptyList()
)