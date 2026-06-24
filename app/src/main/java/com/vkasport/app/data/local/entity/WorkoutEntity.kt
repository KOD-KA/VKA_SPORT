package com.vkasport.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val startTime: Long,

    val endTime: Long? = null,

    val athleteWeight: Float,

    val weightDifference: Float = 0f,

    val daysSinceLastWorkout: Int = 0
)