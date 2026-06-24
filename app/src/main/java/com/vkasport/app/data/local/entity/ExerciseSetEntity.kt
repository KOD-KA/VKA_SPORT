package com.vkasport.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_sets")
data class ExerciseSetEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val exerciseId: Long,

    val setNumber: Int,

    val weight: Float,

    val reps: Int
)