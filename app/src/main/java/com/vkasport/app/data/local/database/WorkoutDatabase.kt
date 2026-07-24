package com.vkasport.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vkasport.app.data.local.dao.*
import com.vkasport.app.data.local.entity.*

/**
 * ПРАВИЛА ИЗМЕНЕНИЯ СХЕМЫ БД (данные не сбрасываются):
 *
 * 1. Меняешь entity (новая колонка/таблица) → повышаешь version на +1
 * 2. СРАЗУ ЖЕ пишешь миграцию в Migrations.kt (MIGRATION_15_16 и т.д.)
 *    и добавляешь её в ALL_MIGRATIONS — иначе приложение упадёт при
 *    первом же запуске после обновления
 * 3. Старые миграции НИКОГДА не редактируются — только добавляются новые
 * 4. Папку app/schemas (JSON-снимки схемы, создаются при сборке) не удалять —
 *    по ним пишутся и проверяются миграции
 */
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
        CustomExerciseEntity::class,
        InProgressWorkoutEntity::class,
        BodyMetricEntity::class,
        UserProfileEntity::class
    ],
    // 14 -> 15: модель упражнений v2 — типы измерений (MeasureType):
    //   custom_exercises.measureType, completed_workout_exercises.measureType,
    //   completed_workout_sets.{seconds,distanceKm,load,speed},
    //   exercise_history.{measureType,bestSeconds,bestDistanceKm}
    // 15 -> 16: таблица body_metrics (вес и замеры тела, этап «тело»)
    // 16 -> 17: таблица user_profile (имя, фото, рост, вес — этап «профиль»)
    version = 17,
    exportSchema = true
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
    abstract fun inProgressWorkoutDao(): InProgressWorkoutDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun userProfileDao(): UserProfileDao
}