package com.vkasport.app.data.repository

import com.vkasport.app.data.local.dao.WorkoutDao
import com.vkasport.app.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val dao: WorkoutDao
) {

    fun getAllWorkouts(): Flow<List<WorkoutEntity>> {
        return dao.getAllWorkouts()
    }

    suspend fun insertWorkout(workout: WorkoutEntity) {
        dao.insertWorkout(workout)
    }

    suspend fun deleteWorkout(workout: WorkoutEntity) {
        dao.deleteWorkout(workout)
    }

    suspend fun getWorkoutById(id: Long): WorkoutEntity? {
        return dao.getWorkoutById(id)
    }
}