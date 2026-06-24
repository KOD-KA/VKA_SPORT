package com.vkasport.app.data.model

import java.time.LocalDateTime



data class ExerciseHistory(

    val exerciseName: String,

    val maxWeight: Float = 0f,

    val maxWeightReps: Int = 0,

    val maxReps: Int = 0,

    val bestVolume: Float = 0f,

    val recordDate: LocalDateTime? = null

)