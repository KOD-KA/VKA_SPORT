package com.vkasport.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vkasport.app.data.local.entity.ExerciseSetEntity
import com.vkasport.app.data.repository.ExerciseSetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ExerciseSetViewModel(
    private val repository: ExerciseSetRepository
) : ViewModel() {

    private val _sets =
        MutableStateFlow<List<ExerciseSetEntity>>(emptyList())

    val sets: StateFlow<List<ExerciseSetEntity>> = _sets

    fun loadSets(exerciseId: Long) {

        viewModelScope.launch {

            repository
                .getSetsForExercise(exerciseId)
                .collect {

                    _sets.value = it
                }
        }
    }

    fun addSet(
        exerciseId: Long,
        setNumber: Int,
        weight: Float,
        reps: Int
    ) {

        viewModelScope.launch {

            repository.insertSet(
                ExerciseSetEntity(
                    exerciseId = exerciseId,
                    setNumber = setNumber,
                    weight = weight,
                    reps = reps
                )
            )
        }
    }

    fun deleteSet(
        set: ExerciseSetEntity
    ) {

        viewModelScope.launch {
            repository.deleteSet(set)
        }
    }
}