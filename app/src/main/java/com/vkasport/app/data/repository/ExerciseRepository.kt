package com.vkasport.app.data.repository

import com.vkasport.app.data.local.dao.ExerciseDao
import com.vkasport.app.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(
    private val dao: ExerciseDao
) {

    fun getExercisesByWorkout(workoutId: Long): Flow<List<ExerciseEntity>> {
        return dao.getExercisesByWorkout(workoutId)
    }

    suspend fun insertExercise(exercise: ExerciseEntity) {
        dao.insertExercise(exercise)
    }

    suspend fun deleteExercise(exercise: ExerciseEntity) {
        dao.deleteExercise(exercise)
    }
}