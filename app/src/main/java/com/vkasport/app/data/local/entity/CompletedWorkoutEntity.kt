package com.vkasport.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "completed_workouts")
data class CompletedWorkoutEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val date: Long,

    val duration: Long? = null,

    val athleteWeight: Float? = null,

    val muscleGroup: String,

    // Заметки о тренировке (самочувствие и т.п.), добавляются на экране
    // итогов после завершения
    val notes: String? = null

)