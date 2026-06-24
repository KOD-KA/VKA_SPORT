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
                .fallbackToDestructiveMigration()
                .build()

            INSTANCE = instance
            instance
        }
    }
}