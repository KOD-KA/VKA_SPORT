package com.vkasport.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_history")
data class ExerciseHistoryEntity(

    @PrimaryKey
    val exerciseName: String,

    val maxWeight: Float,
    val maxWeightReps: Int,
    val maxReps: Int,
    val bestVolume: Float,

    val recordDate: Long? = null,
    val athleteWeight: Float? = null,
    // Подход, давший лучший объём
    val bestVolumeWeight: Float? = null,
    val bestVolumeReps: Int? = null
)