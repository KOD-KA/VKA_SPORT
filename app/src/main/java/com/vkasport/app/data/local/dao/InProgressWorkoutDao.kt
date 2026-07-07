package com.vkasport.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vkasport.app.data.local.entity.InProgressWorkoutEntity

@Dao
interface InProgressWorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: InProgressWorkoutEntity)

    @Query("SELECT * FROM in_progress_workout WHERE id = 1")
    suspend fun get(): InProgressWorkoutEntity?

    @Query("DELETE FROM in_progress_workout")
    suspend fun clear()
}