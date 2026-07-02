package com.vkasport.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vkasport.app.data.local.dao.*
import com.vkasport.app.data.local.entity.*

@Database(
    entities = [
        WorkoutEntity::class,
        ExerciseEntity::class,
        ExerciseSetEntity::class,
        CompletedWorkoutEntity::class,
        CompletedWorkoutExerciseEntity::class,
        CompletedWorkoutSetEntity::class,
        ExerciseHistoryEntity::class,
        PlannedWorkoutEntity::class,
        PlannedExerciseEntity::class,
        CustomExerciseEntity::class
    ],
    version = 12,          // 11 -> 12: добавлена таблица custom_exercises
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun workoutHistoryDao(): WorkoutHistoryDao
    abstract fun exerciseHistoryDao(): ExerciseHistoryDao
    abstract fun completedWorkoutExerciseDao(): CompletedWorkoutExerciseDao
    abstract fun completedWorkoutSetDao(): CompletedWorkoutSetDao
    abstract fun plannedWorkoutDao(): PlannedWorkoutDao
    abstract fun customExerciseDao(): CustomExerciseDao
}