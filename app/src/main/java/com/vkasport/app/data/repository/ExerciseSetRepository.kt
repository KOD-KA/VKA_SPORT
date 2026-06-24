package com.vkasport.app.data.repository

import com.vkasport.app.data.local.dao.ExerciseSetDao
import com.vkasport.app.data.local.entity.ExerciseSetEntity
import kotlinx.coroutines.flow.Flow

class ExerciseSetRepository(
    private val dao: ExerciseSetDao
) {

    fun getSetsForExercise(
        exerciseId: Long
    ): Flow<List<ExerciseSetEntity>> {
        return dao.getSetsForExercise(exerciseId)
    }

    suspend fun insertSet(
        set: ExerciseSetEntity
    ) {
        dao.insertSet(set)
    }

    suspend fun deleteSet(
        set: ExerciseSetEntity
    ) {
        dao.deleteSet(set)
    }
}