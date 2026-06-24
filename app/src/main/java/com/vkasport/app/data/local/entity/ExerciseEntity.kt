package com.vkasport.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val workoutId: Long,

    val name: String,

    val muscleGroup: String,

    val lastWeight: Float = 0f,

    val recordWeight: Float = 0f,

    val gifUrl: String? = null
)