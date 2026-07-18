package com.vkasport.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vkasport.app.data.local.dao.*
import com.vkasport.app.data.local.entity.*

/**
 * ПРАВИЛА ИЗМЕНЕНИЯ СХЕМЫ БД (с этапа «миграции» данные больше не сбрасываются):
 *
 * 1. Меняешь entity (новая колонка/таблица) → повышаешь version на +1
 * 2. СРАЗУ ЖЕ пишешь миграцию в Migrations.kt (MIGRATION_14_15 и т.д.)
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
        InProgressWorkoutEntity::class
    ],
    version = 14,          // 13 -> 14: notes в completed_workouts + таблица in_progress_workout
    // ИЗМЕНЕНО: true — Room при каждой сборке сохраняет JSON-снимок схемы
    // в app/schemas/ (путь задан в build.gradle.kts). Версия 14 —
    // «базовая»: от неё пишутся все будущие миграции.
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
}