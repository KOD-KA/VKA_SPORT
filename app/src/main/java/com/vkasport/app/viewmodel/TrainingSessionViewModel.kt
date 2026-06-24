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

    private val _state =
        MutableStateFlow(CurrentWorkoutState())

    private val _completedWorkouts =
        MutableStateFlow<List<CompletedWorkout>>(emptyList())

    private val _exerciseHistory =
        MutableStateFlow<Map<String, ExerciseHistory>>(emptyMap())

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

    fun addSetToExercise(
        exerciseName: String,
        weight: Float,
        reps: Int
    ) {
        val updatedExercises = _state.value.selectedExercises.map { exercise ->
            if (exercise.name == exerciseName) {
                exercise.copy(sets = exercise.sets + WorkoutSet(weight = weight, reps = reps))
            } else {
                exercise
            }
        }

        _state.value = _state.value.copy(selectedExercises = updatedExercises)

        updateExerciseStats(
            exerciseName = exerciseName,
            weight = weight,
            reps = reps
        )
    }

    fun updateAthleteWeight(weight: Float) {
        _state.value = _state.value.copy(athleteWeight = weight)
    }

    fun startTraining() {
        _state.value = _state.value.copy(
            trainingStarted = true,
            currentScreen = "weight"
        )
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
                    duration = Duration.between(
                        currentState.workoutStartTime,
                        LocalDateTime.now()
                    ).toMinutes(),
                    athleteWeight = currentState.athleteWeight,
                    muscleGroup = currentState.selectedMuscleGroup?.title ?: "Не выбрано"
                )
            )

            currentState.selectedExercises.forEach { exercise ->

                database.completedWorkoutExerciseDao().insert(
                    CompletedWorkoutExerciseEntity(
                        workoutId = workoutId,
                        exerciseName = exercise.name
                    )
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

        val durationMinutes = Duration.between(
            currentState.workoutStartTime,
            LocalDateTime.now()
        ).toMinutes()

        // Добавляем в память сразу — exercises здесь корректны,
        // т.к. берём из текущей сессии
        val completedWorkout = CompletedWorkout(
            dateTime = currentState.workoutStartTime,
            athleteWeight = currentState.athleteWeight,
            muscleGroup = currentState.selectedMuscleGroup?.title ?: "Не выбрано",
            exercises = currentState.selectedExercises,
            durationMinutes = durationMinutes
        )

        _completedWorkouts.value = _completedWorkouts.value + completedWorkout

        _state.value = _state.value.copy(currentScreen = "summary")
    }

    // ==================== EXERCISE STATS ====================

    fun updateExerciseStats(
        exerciseName: String,
        weight: Float,
        reps: Int
    ) {
        val volume = weight * reps
        val current = _exerciseHistory.value[exerciseName]

        val isNewRecord = weight > (current?.maxWeight ?: 0f)

        val updated = ExerciseHistory(
            exerciseName = exerciseName,
            maxWeight = maxOf(current?.maxWeight ?: 0f, weight),
            maxWeightReps = when {
                isNewRecord -> reps
                else -> current?.maxWeightReps ?: 0
            },
            maxReps = maxOf(current?.maxReps ?: 0, reps),
            bestVolume = maxOf(current?.bestVolume ?: 0f, volume),
            recordDate = if (isNewRecord) {
                LocalDateTime.now()
            } else {
                current?.recordDate ?: LocalDateTime.now()
            }
        )

        _exerciseHistory.value = _exerciseHistory.value + (exerciseName to updated)

        viewModelScope.launch {
            database.exerciseHistoryDao().save(
                ExerciseHistoryEntity(
                    exerciseName = updated.exerciseName,
                    maxWeight = updated.maxWeight,
                    maxWeightReps = updated.maxWeightReps,
                    maxReps = updated.maxReps,
                    bestVolume = updated.bestVolume,
                    // ИСПРАВЛЕНО: сохраняем время в локальном часовом поясе
                    recordDate = updated.recordDate
                        ?.atZone(ZoneId.systemDefault())
                        ?.toInstant()
                        ?.toEpochMilli(),
                    // ИСПРАВЛЕНО: сохраняем вес атлета при рекорде
                    athleteWeight = _state.value.athleteWeight
                )
            )
        }
    }

    // ==================== LOAD FROM DATABASE ====================

    fun loadArchiveFromDatabase() {

        viewModelScope.launch {

            val workoutEntities = database.workoutHistoryDao().getAllWorkouts()

            val completedWorkouts = workoutEntities.map { entity ->

                // Загружаем упражнения для тренировки
                val exerciseEntities = database.completedWorkoutExerciseDao()
                    .getByWorkout(entity.id)

                // Загружаем ВСЕ сеты тренировки одним запросом (эффективнее N+1)
                val allSets = database.completedWorkoutSetDao()
                    .getByWorkout(entity.id)

                // Группируем сеты по названию упражнения
                val setsByExercise = allSets.groupBy { it.exerciseName }

                // Строим список упражнений с подходами
                val exercises = exerciseEntities.map { exerciseEntity ->
                    val sets = setsByExercise[exerciseEntity.exerciseName] ?: emptyList()
                    WorkoutExercise(
                        name = exerciseEntity.exerciseName,
                        sets = sets.map { setEntity ->
                            WorkoutSet(
                                weight = setEntity.weight,
                                reps = setEntity.reps
                            )
                        }
                    )
                }

                CompletedWorkout(
                    id = entity.id,
                    // ИСПРАВЛЕНО: используем локальный часовой пояс вместо UTC
                    dateTime = Instant.ofEpochMilli(entity.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime(),
                    athleteWeight = entity.athleteWeight,
                    muscleGroup = entity.muscleGroup,
                    // ИСПРАВЛЕНО: exercises больше не emptyList()
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

            _exerciseHistory.value = data.associateBy { entity ->
                entity.exerciseName
            }.mapValues { entry ->

                val entity = entry.value

                ExerciseHistory(
                    exerciseName = entity.exerciseName,
                    maxWeight = entity.maxWeight,
                    maxWeightReps = entity.maxWeightReps,
                    maxReps = entity.maxReps,
                    bestVolume = entity.bestVolume,
                    // ИСПРАВЛЕНО: recordDate теперь восстанавливается из БД
                    recordDate = entity.recordDate?.let { epochMillis ->
                        Instant.ofEpochMilli(epochMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    }
                )
            }
        }
    }

    // ==================== COMPUTED GETTERS ====================

    fun getDaysSinceLastWorkout(): Long? {
        val workouts = _completedWorkouts.value
        if (workouts.isEmpty()) return null
        return Duration.between(workouts.last().dateTime, LocalDateTime.now()).toDays()
    }

    fun getWeightDifference(): Float? {
        val workouts = _completedWorkouts.value
        if (workouts.size < 2) return null
        val currentWeight = workouts.last().athleteWeight ?: return null
        val previousWeight = workouts[workouts.size - 2].athleteWeight ?: return null
        return currentWeight - previousWeight
    }

    fun getExerciseRecord(exerciseName: String): ExerciseHistory? {
        return _exerciseHistory.value[exerciseName]
    }

    fun getLastExerciseResult(exerciseName: String): WorkoutSet? {
        for (workout in _completedWorkouts.value.asReversed()) {
            val exercise = workout.exercises.find { it.name == exerciseName }
            if (exercise != null && exercise.sets.isNotEmpty()) {
                return exercise.sets.last()
            }
        }
        return null
    }

    fun isExerciseRecord(
        exerciseName: String,
        weight: Float,
        reps: Int
    ): Boolean {
        val record = _exerciseHistory.value[exerciseName] ?: return false
        return weight >= record.maxWeight && reps >= record.maxWeightReps
    }

    fun getPreviousSet(exerciseName: String, index: Int): WorkoutSet? {
        for (workout in _completedWorkouts.value.asReversed()) {
            val exercise = workout.exercises.find { it.name == exerciseName }
            if (exercise != null && exercise.sets.size > index) {
                return exercise.sets[index]
            }
        }
        return null
    }

    fun getCurrentExercisesCount(): Int {
        return _state.value.selectedExercises.size
    }

    fun getCurrentSetsCount(): Int {
        return _state.value.selectedExercises.sumOf { it.sets.size }
    }

    fun getCurrentVolume(): Float {
        return _state.value.selectedExercises.sumOf { exercise ->
            exercise.sets.sumOf { set ->
                (set.weight * set.reps).toDouble()
            }
        }.toFloat()
    }
}