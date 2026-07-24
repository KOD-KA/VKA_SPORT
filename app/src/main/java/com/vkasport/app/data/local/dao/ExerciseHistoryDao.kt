package com.vkasport.app.data.local.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vkasport.app.data.local.entity.ExerciseHistoryEntity


@Dao
interface ExerciseHistoryDao {


    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun save(
        record: ExerciseHistoryEntity
    )


    @Query(
        "SELECT * FROM exercise_history"
    )
    suspend fun getAll():
            List<ExerciseHistoryEntity>

    // exerciseName — первичный ключ; переименование безопасно, если нового
    // имени ещё нет в таблице (проверка коллизии — на стороне ViewModel)
    @Query("UPDATE exercise_history SET exerciseName = :newName WHERE exerciseName = :oldName")
    suspend fun renameExercise(oldName: String, newName: String)

}