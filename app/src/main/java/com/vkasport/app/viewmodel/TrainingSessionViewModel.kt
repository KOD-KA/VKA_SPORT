package com.vkasport.app.viewmodel

import androidx.lifecycle.ViewModel
import com.vkasport.app.data.model.CurrentWorkoutState
import com.vkasport.app.data.model.MuscleGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.vkasport.app.data.model.WorkoutExercise
import com.vkasport.app.data.model.WorkoutSet
import com.vkasport.app.data.model.CompletedWorkout
import kotlinx.coroutines.flow.asStateFlow
import com.vkasport.app.data.model.ExerciseHistory
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.vkasport.app.data.local.database.WorkoutDatabase
import com.vkasport.app.data.local.entity.CompletedWorkoutEntity
import com.vkasport.app.data.local.entity.ExerciseHistoryEntity
import com.vkasport.app.data.local.entity.CompletedWorkoutExerciseEntity
import com.vkasport.app.data.local.entity.CompletedWorkoutSetEntity

class TrainingSessionViewModel(
    private val database: WorkoutDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(CurrentWorkoutState())
    private val _completedWorkouts = MutableStateFlow<List<CompletedWorkout>>(emptyList())
    private val _exerciseHistory = MutableStateFlow<Map<String, ExerciseHistory>>(emptyMap())

    val exerciseHistory = _exerciseHistory.asStateFlow()
    val completedWorkouts = _completedWorkouts.asStateFlow()
    val state: StateFlow<CurrentWorkoutState> = _state

    // ==================== WORKOUT ACTIONS ====================

    fun selectMuscleGroup(group: MuscleGroup) {
        _state.value = _state.value.copy(selectedMuscleGroup = group)
    }

    fun addExercise(exerciseName: String) {
        val exercise = WorkoutExercise(name = exerciseName)
        _state.value = _state.value.copy(
            selectedExercises = _state.value.selectedExercises + exercise
        )
    }

    fun addSetToExercise(exerciseName: String, weight: Float, reps: Int) {
        val updatedExercises = _state.value.selectedExercises.map { exercise ->
            if (exercise.name == exerciseName)
                exercise.copy(sets = exercise.sets + WorkoutSet(weight = weight, reps = reps))
            else exercise
        }
        _state.value = _state.value.copy(selectedExercises = updatedExercises)
        updateExerciseStats(exerciseName, weight, reps)
    }

    fun updateAthleteWeight(weight: Float) {
        _state.value = _state.value.copy(athleteWeight = weight)
    }

    fun startTraining() {
        _state.value = _state.value.copy(trainingStarted = true, currentScreen = "weight")
    }

    fun setCurrentScreen(screen: String) {
        _state.value = _state.value.copy(currentScreen = screen)
    }

    fun resetWorkout() {
        _state.value = CurrentWorkoutState()
        loadArchiveFromDatabase()
        loadRecordsFromDatabase()
    }

    // ==================== FINISH WORKOUT ====================

    fun finishCurrentWorkout() {
        val currentState = _state.value

        viewModelScope.launch {
            val workoutId = database.workoutHistoryDao().insertWorkout(
                CompletedWorkoutEntity(
                    date = System.currentTimeMillis(),
                    duration = Duration.between(currentState.workoutStartTime, LocalDateTime.now()).toMinutes(),
                    athleteWeight = currentState.athleteWeight,
                    muscleGroup = currentState.selectedMuscleGroup?.title ?: "Не выбрано"
                )
            )
            currentState.selectedExercises.forEach { exercise ->
                database.completedWorkoutExerciseDao().insert(
                    CompletedWorkoutExerciseEntity(workoutId = workoutId, exerciseName = exercise.name)
                )
                exercise.sets.forEach { set ->
                    database.completedWorkoutSetDao().insert(
                        CompletedWorkoutSetEntity(
                            workoutId = workoutId,
                            exerciseName = exercise.name,
                            weight = set.weight,
                            reps = set.reps
                        )
                    )
                }
            }
        }

        val completedWorkout = CompletedWorkout(
            dateTime = currentState.workoutStartTime,
            athleteWeight = currentState.athleteWeight,
            muscleGroup = currentState.selectedMuscleGroup?.title ?: "Не выбрано",
            exercises = currentState.selectedExercises,
            durationMinutes = Duration.between(currentState.workoutStartTime, LocalDateTime.now()).toMinutes()
        )
        _completedWorkouts.value = _completedWorkouts.value + completedWorkout
        _state.value = _state.value.copy(currentScreen = "summary")
    }

    // ==================== EXERCISE STATS ====================

    fun updateExerciseStats(exerciseName: String, weight: Float, reps: Int) {
        val volume  = weight * reps
        val current = _exerciseHistory.value[exerciseName]

        val isNewWeightRecord  = weight  > (current?.maxWeight  ?: 0f)
        val isNewVolumeRecord  = volume  > (current?.bestVolume ?: 0f)

        val updated = ExerciseHistory(
            exerciseName     = exerciseName,
            maxWeight        = maxOf(current?.maxWeight ?: 0f, weight),
            maxWeightReps    = if (isNewWeightRecord) reps else (current?.maxWeightReps ?: 0),
            maxReps          = maxOf(current?.maxReps ?: 0, reps),
            bestVolume       = maxOf(current?.bestVolume ?: 0f, volume),
            // Дата — обновляем только при новом рекорде веса
            recordDate       = if (isNewWeightRecord) LocalDateTime.now()
            else current?.recordDate ?: LocalDateTime.now(),
            // Вес атлета в момент рекорда
            athleteWeight    = if (isNewWeightRecord) _state.value.athleteWeight
            else current?.athleteWeight,
            // Разбивка объёма: подход, давший лучший объём
            bestVolumeWeight = if (isNewVolumeRecord) weight else current?.bestVolumeWeight,
            bestVolumeReps   = if (isNewVolumeRecord) reps   else current?.bestVolumeReps
        )

        _exerciseHistory.value = _exerciseHistory.value + (exerciseName to updated)

        viewModelScope.launch {
            database.exerciseHistoryDao().save(
                ExerciseHistoryEntity(
                    exerciseName     = updated.exerciseName,
                    maxWeight        = updated.maxWeight,
                    maxWeightReps    = updated.maxWeightReps,
                    maxReps          = updated.maxReps,
                    bestVolume       = updated.bestVolume,
                    recordDate       = updated.recordDate
                        ?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                    athleteWeight    = updated.athleteWeight,
                    bestVolumeWeight = updated.bestVolumeWeight,
                    bestVolumeReps   = updated.bestVolumeReps
                )
            )
        }
    }

    // ==================== LOAD FROM DATABASE ====================

    fun loadArchiveFromDatabase() {
        viewModelScope.launch {
            val workoutEntities = database.workoutHistoryDao().getAllWorkouts()
            val completedWorkouts = workoutEntities.map { entity ->
                val exerciseEntities = database.completedWorkoutExerciseDao().getByWorkout(entity.id)
                val allSets = database.completedWorkoutSetDao().getByWorkout(entity.id)
                val setsByExercise = allSets.groupBy { it.exerciseName }
                val exercises = exerciseEntities.map { exerciseEntity ->
                    val sets = setsByExercise[exerciseEntity.exerciseName] ?: emptyList()
                    WorkoutExercise(
                        name = exerciseEntity.exerciseName,
                        sets = sets.map { WorkoutSet(weight = it.weight, reps = it.reps) }
                    )
                }
                CompletedWorkout(
                    id = entity.id,
                    dateTime = Instant.ofEpochMilli(entity.date)
                        .atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    athleteWeight = entity.athleteWeight,
                    muscleGroup = entity.muscleGroup,
                    exercises = exercises,
                    durationMinutes = entity.duration ?: 0
                )
            }
            _completedWorkouts.value = completedWorkouts
        }
    }

    fun loadRecordsFromDatabase() {
        viewModelScope.launch {
            val data = database.exerciseHistoryDao().getAll()
            _exerciseHistory.value = data.associateBy { it.exerciseName }.mapValues { (_, entity) ->
                ExerciseHistory(
                    exerciseName     = entity.exerciseName,
                    maxWeight        = entity.maxWeight,
                    maxWeightReps    = entity.maxWeightReps,
                    maxReps          = entity.maxReps,
                    bestVolume       = entity.bestVolume,
                    recordDate       = entity.recordDate?.let { ms ->
                        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    },
                    athleteWeight    = entity.athleteWeight,
                    bestVolumeWeight = entity.bestVolumeWeight,
                    bestVolumeReps   = entity.bestVolumeReps
                )
            }
        }
    }

    // ==================== COMPUTED GETTERS ====================

    fun getDaysSinceLastWorkout(): Long? {
        val w = _completedWorkouts.value
        if (w.isEmpty()) return null
        return Duration.between(w.last().dateTime, LocalDateTime.now()).toDays()
    }

    fun getWeightDifference(): Float? {
        val w = _completedWorkouts.value
        if (w.size < 2) return null
        return (w.last().athleteWeight ?: return null) - (w[w.size - 2].athleteWeight ?: return null)
    }

    fun getExerciseRecord(exerciseName: String) = _exerciseHistory.value[exerciseName]

    fun getLastExerciseResult(exerciseName: String): WorkoutSet? {
        for (workout in _completedWorkouts.value.asReversed()) {
            val ex = workout.exercises.find { it.name == exerciseName }
            if (ex != null && ex.sets.isNotEmpty()) return ex.sets.last()
        }
        return null
    }

    fun isExerciseRecord(exerciseName: String, weight: Float, reps: Int): Boolean {
        val record = _exerciseHistory.value[exerciseName] ?: return false
        return weight >= record.maxWeight && reps >= record.maxWeightReps
    }

    fun getPreviousSet(exerciseName: String, index: Int): WorkoutSet? {
        for (workout in _completedWorkouts.value.asReversed()) {
            val ex = workout.exercises.find { it.name == exerciseName }
            if (ex != null && ex.sets.size > index) return ex.sets[index]
        }
        return null
    }

    fun getCurrentExercisesCount() = _state.value.selectedExercises.size
    fun getCurrentSetsCount() = _state.value.selectedExercises.sumOf { it.sets.size }
    fun getCurrentVolume() = _state.value.selectedExercises
        .sumOf { ex -> ex.sets.sumOf { (it.weight * it.reps).toDouble() } }.toFloat()
}