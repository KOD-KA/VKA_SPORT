package com.vkasport.app.data.model

data class WorkoutExercise(
    val name: String,
    // Группа мышц, к которой относится упражнение в текущей тренировке.
    // Null для старых записей архива, где это поле не хранилось.
    val muscleGroup: MuscleGroup? = null,
    val sets: List<WorkoutSet> = emptyList()
)