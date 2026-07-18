package com.vkasport.app.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Все миграции схемы БД. Версия 14 — базовая: от неё начинается история.
 *
 * КАК ДОБАВИТЬ МИГРАЦИЮ (пример — будущий переход 14 -> 15):
 *
 * 1. Изменить entity, например добавить колонку в CompletedWorkoutEntity:
 *        val mood: String? = null
 *
 * 2. В WorkoutDatabase.kt повысить version = 15
 *
 * 3. Раскомментировать и подправить шаблон ниже:
 *
 *    val MIGRATION_14_15 = object : Migration(14, 15) {
 *        override fun migrate(db: SupportSQLiteDatabase) {
 *            // SQL должен приводить СТАРУЮ базу ровно к НОВОЙ схеме.
 *            // Новые колонки в Room по умолчанию nullable (TEXT/INTEGER/REAL
 *            // без NOT NULL). Если в entity поле не-null — обязательно
 *            // NOT NULL DEFAULT <значение>.
 *            db.execSQL("ALTER TABLE completed_workouts ADD COLUMN mood TEXT")
 *        }
 *    }
 *
 * 4. Добавить её в ALL_MIGRATIONS:  arrayOf(MIGRATION_14_15)
 *
 * 5. Если при запуске после миграции падает
 *    IllegalStateException "Migration didn't properly handle ..." —
 *    Room в сообщении показывает две схемы (expected/found): сравнить
 *    и поправить SQL. Сверяться удобно с JSON-снимками в app/schemas/.
 *
 * ПРАВИЛА:
 *  - одна миграция на каждый шаг версии (14->15, 15->16, ...)
 *  - старые миграции не редактировать и не удалять
 *  - данных пользователя они не стирают — в этом весь смысл
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    // сюда добавлять будущие миграции: MIGRATION_14_15, MIGRATION_15_16, ...
)