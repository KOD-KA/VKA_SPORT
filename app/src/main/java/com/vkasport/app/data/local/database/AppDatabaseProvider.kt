package com.vkasport.app.data.local.database

import android.content.Context
import androidx.room.Room

object AppDatabaseProvider {

    @Volatile
    private var INSTANCE: WorkoutDatabase? = null

    fun getDatabase(context: Context): WorkoutDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                WorkoutDatabase::class.java,
                "vka_sport_database"
            )
                // ИЗМЕНЕНО: миграции вместо сброса БД.
                // Раньше стоял fallbackToDestructiveMigration() — при каждом
                // повышении версии база пересоздавалась с нуля и все данные
                // пропадали. Теперь Room выполняет скрипты из Migrations.kt,
                // данные при обновлении приложения сохраняются.
                .addMigrations(*ALL_MIGRATIONS)
                // Единственное исключение — древние версии времён разработки
                // (1–13): для них миграций нет и не будет, при обновлении с
                // них база пересоздастся ОДИН раз. Начиная с версии 14 данные
                // больше никогда не сбрасываются.
                .fallbackToDestructiveMigrationFrom(
                    true, // dropAllTables при сбросе со старых версий
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13
                )
                .build()

            INSTANCE = instance
            instance
        }
    }
}