package com.vkasport.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vkasport.app.data.local.entity.ExerciseSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(
        set: ExerciseSetEntity
    )

    @Delete
    suspend fun deleteSet(
        set: ExerciseSetEntity
    )

    @Query(
        "SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId ORDER BY setNumber ASC"
    )
    fun getSetsForExercise(
        exerciseId: Long
    ): Flow<List<ExerciseSetEntity>>
}