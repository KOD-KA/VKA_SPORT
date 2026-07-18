package com.vkasport.app.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Все миграции схемы БД. Версия 14 — базовая: от неё начинается история.
 *
 * ПРАВИЛА:
 *  - одна миграция на каждый шаг версии (14->15, 15->16, ...)
 *  - старые миграции не редактировать и не удалять
 *  - новые колонки добавляем NULLABLE и БЕЗ DEFAULT в SQL — тогда Room
 *    не сверяет default-значения и миграции проще писать без ошибок
 *    (null трактуется в коде как «старое поведение»)
 *  - данных пользователя миграции не стирают — в этом весь смысл
 */

/**
 * 14 -> 15: модель упражнений v2 — типы измерений (MeasureType).
 * Все новые колонки nullable, старые данные продолжают работать как
 * WEIGHT_REPS (вес × повторы).
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // как считается своё упражнение (MeasureType.name, null = WEIGHT_REPS)
        db.execSQL("ALTER TABLE custom_exercises ADD COLUMN measureType TEXT")

        // как считалось упражнение в завершённой тренировке
        db.execSQL("ALTER TABLE completed_workout_exercises ADD COLUMN measureType TEXT")

        // поля подхода для новых типов
        db.execSQL("ALTER TABLE completed_workout_sets ADD COLUMN seconds INTEGER")
        db.execSQL("ALTER TABLE completed_workout_sets ADD COLUMN distanceKm REAL")
        db.execSQL("ALTER TABLE completed_workout_sets ADD COLUMN load REAL")
        db.execSQL("ALTER TABLE completed_workout_sets ADD COLUMN speed REAL")

        // рекорды для новых типов
        db.execSQL("ALTER TABLE exercise_history ADD COLUMN measureType TEXT")
        db.execSQL("ALTER TABLE exercise_history ADD COLUMN bestSeconds INTEGER")
        db.execSQL("ALTER TABLE exercise_history ADD COLUMN bestDistanceKm REAL")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_14_15
    // сюда добавлять будущие миграции: MIGRATION_15_16, ...
)