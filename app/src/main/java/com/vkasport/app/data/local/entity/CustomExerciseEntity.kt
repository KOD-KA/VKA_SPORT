package com.vkasport.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_exercises")
data class CustomExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // Хранится как MuscleGroup.name (стабильный ключ enum), не как
    // локализованный title, чтобы не сломаться при смене текста.
    val muscleGroup: String
)