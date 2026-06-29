package com.vkasport.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planned_workouts")
data class PlannedWorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,        // LocalDate.toEpochDay()
    val hour: Int,
    val minute: Int,
    val muscleGroup: String
)