package com.vkasport.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vkasport.app.data.local.dao.WorkoutDao
import com.vkasport.app.data.local.entity.WorkoutEntity
import com.vkasport.app.data.local.dao.ExerciseDao
import com.vkasport.app.data.local.dao.ExerciseHistoryDao
import com.vkasport.app.data.local.entity.ExerciseEntity
import com.vkasport.app.data.local.entity.ExerciseSetEntity
import com.vkasport.app.data.local.dao.ExerciseSetDao
import com.vkasport.app.data.local.dao.WorkoutHistoryDao
import com.vkasport.app.data.local.entity.CompletedWorkoutEntity
import com.vkasport.app.data.local.entity.CompletedWorkoutExerciseEntity
import com.vkasport.app.data.local.entity.CompletedWorkoutSetEntity
import com.vkasport.app.data.local.entity.ExerciseHistoryEntity
import com.vkasport.app.data.local.dao.CompletedWorkoutExerciseDao
import com.vkasport.app.data.local.dao.CompletedWorkoutSetDao

@Database(
    entities = [
        WorkoutEntity::class,
        ExerciseEntity::class,
        ExerciseSetEntity::class,
        CompletedWorkoutEntity::class,
        CompletedWorkoutExerciseEntity::class,
        CompletedWorkoutSetEntity::class,
        ExerciseHistoryEntity::class
    ],
    version = 10,           // 9 → 10: добавлены bestVolumeWeight, bestVolumeReps
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
}