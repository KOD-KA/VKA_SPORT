package com.vkasport.app.viewmodel

import androidx.lifecycle.ViewModel
import com.vkasport.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.vkasport.app.data.local.database.WorkoutDatabase
import com.vkasport.app.data.local.entity.*

class TrainingSessionViewModel(private val database: WorkoutDatabase) : ViewModel() {

    private val _state            = MutableStateFlow(CurrentWorkoutState())
    private val _completedWorkouts= MutableStateFlow<List<CompletedWorkout>>(emptyList())
    private val _exerciseHistory  = MutableStateFlow<Map<String, ExerciseHistory>>(emptyMap())
    private val _plannedWorkouts  = MutableStateFlow<List<PlannedWorkout>>(emptyList())
    private val _customExercises  = MutableStateFlow<List<CustomExercise>>(emptyList())

    val state: StateFlow<CurrentWorkoutState>     = _state
    val completedWorkouts = _completedWorkouts.asStateFlow()
    val exerciseHistory   = _exerciseHistory.asStateFlow()
    val plannedWorkouts   = _plannedWorkouts.asStateFlow()
    val customExercises   = _customExercises.asStateFlow()

    // ==================== WORKOUT ACTIONS ====================

    fun selectMuscleGroup(group: MuscleGroup) { _state.value = _state.value.copy(selectedMuscleGroup = group) }

    fun addExercise(name: String) {
        val group = _state.value.selectedMuscleGroup ?: return
        val exercise = WorkoutExercise(name = name, muscleGroup = group)
        _state.value = _state.value.copy(selectedExercises = _state.value.selectedExercises + exercise)
    }

    // Убрать упражнение из текущей тренировки (ещё не сохранённой)
    fun removeExercise(name: String) {
        _state.value = _state.value.copy(
            selectedExercises = _state.value.selectedExercises.filterNot { it.name == name }
        )
    }

    // Убрать целую группу мышц (и все её упражнения) из текущей тренировки
    fun removeMuscleGroup(group: MuscleGroup) {
        _state.value = _state.value.copy(
            selectedExercises = _state.value.selectedExercises.filterNot { it.muscleGroup == group }
        )
    }

    fun updateAthleteWeight(w: Float) { _state.value = _state.value.copy(athleteWeight = w) }
    fun startTraining() { _state.value = _state.value.copy(trainingStarted = true, currentScreen = "weight") }
    fun setCurrentScreen(s: String) { _state.value = _state.value.copy(currentScreen = s) }

    fun addSetToExercise(exerciseName: String, weight: Float, reps: Int) {
        val updated = _state.value.selectedExercises.map { ex ->
            if (ex.name == exerciseName) ex.copy(sets = ex.sets + WorkoutSet(weight, reps)) else ex
        }
        _state.value = _state.value.copy(selectedExercises = updated)
        updateExerciseStats(exerciseName, weight, reps)
    }

    fun resetWorkout() {
        _state.value = CurrentWorkoutState()
        loadArchiveFromDatabase()
        loadRecordsFromDatabase()
    }

    // ==================== ПОВТОР ГРУППЫ ИЗ ПРОШЛОЙ ТРЕНИРОВКИ ====================

    // Добавляет в текущую тренировку упражнения указанной группы мышц из
    // последней тренировки, где эта группа встречалась (без подходов —
    // только состав упражнений, шаблон).
    fun repeatLastMuscleGroup(group: MuscleGroup) {
        val lastWithGroup = _completedWorkouts.value.asReversed()
            .firstOrNull { workout -> workout.exercises.any { it.muscleGroup == group } }
            ?: return

        val alreadyAdded = _state.value.selectedExercises.map { it.name }.toSet()
        val toAdd = lastWithGroup.exercises
            .filter { it.muscleGroup == group && it.name !in alreadyAdded }
            .map { WorkoutExercise(name = it.name, muscleGroup = group) }

        if (toAdd.isEmpty()) return

        _state.value = _state.value.copy(selectedExercises = _state.value.selectedExercises + toAdd)
    }

    // Список групп мышц, которые встречались в предыдущих тренировках —
    // используется чтобы предложить пользователю, что можно повторить
    fun getRepeatableMuscleGroups(): List<MuscleGroup> {
        return _completedWorkouts.value
            .flatMap { it.exercises.mapNotNull { ex -> ex.muscleGroup } }
            .distinct()
    }

    // ==================== FINISH WORKOUT ====================

    fun finishCurrentWorkout() {
        val s = _state.value

        val usedGroups = s.selectedExercises.mapNotNull { it.muscleGroup }.distinct()
        val muscleGroupSummary =
            if (usedGroups.isNotEmpty()) usedGroups.joinToString(", ") { it.title }
            else s.selectedMuscleGroup?.title ?: "Не выбрано"

        viewModelScope.launch {
            val wid = database.workoutHistoryDao().insertWorkout(
                CompletedWorkoutEntity(
                    date = System.currentTimeMillis(),
                    duration = Duration.between(s.workoutStartTime, LocalDateTime.now()).toMinutes(),
                    athleteWeight = s.athleteWeight,
                    muscleGroup = muscleGroupSummary
                )
            )
            s.selectedExercises.forEach { ex ->
                database.completedWorkoutExerciseDao().insert(CompletedWorkoutExerciseEntity(workoutId = wid, exerciseName = ex.name))
                ex.sets.forEach { set -> database.completedWorkoutSetDao().insert(CompletedWorkoutSetEntity(workoutId = wid, exerciseName = ex.name, weight = set.weight, reps = set.reps)) }
            }
        }
        _completedWorkouts.value = _completedWorkouts.value + CompletedWorkout(
            dateTime = s.workoutStartTime, athleteWeight = s.athleteWeight,
            muscleGroup = muscleGroupSummary,
            exercises = s.selectedExercises,
            durationMinutes = Duration.between(s.workoutStartTime, LocalDateTime.now()).toMinutes()
        )
        _state.value = _state.value.copy(currentScreen = "summary")
    }

    // ==================== EXERCISE STATS ====================

    fun updateExerciseStats(exerciseName: String, weight: Float, reps: Int) {
        val volume = weight * reps
        val cur = _exerciseHistory.value[exerciseName]
        val isNewWeight  = weight > (cur?.maxWeight  ?: 0f)
        val isNewVolume  = volume > (cur?.bestVolume ?: 0f)
        val updated = ExerciseHistory(
            exerciseName     = exerciseName,
            maxWeight        = maxOf(cur?.maxWeight ?: 0f, weight),
            maxWeightReps    = if (isNewWeight) reps else (cur?.maxWeightReps ?: 0),
            maxReps          = maxOf(cur?.maxReps ?: 0, reps),
            bestVolume       = maxOf(cur?.bestVolume ?: 0f, volume),
            recordDate       = if (isNewWeight) LocalDateTime.now() else cur?.recordDate ?: LocalDateTime.now(),
            athleteWeight    = if (isNewWeight) _state.value.athleteWeight else cur?.athleteWeight,
            bestVolumeWeight = if (isNewVolume) weight else cur?.bestVolumeWeight,
            bestVolumeReps   = if (isNewVolume) reps   else cur?.bestVolumeReps
        )
        _exerciseHistory.value = _exerciseHistory.value + (exerciseName to updated)
        viewModelScope.launch {
            database.exerciseHistoryDao().save(ExerciseHistoryEntity(
                exerciseName = updated.exerciseName, maxWeight = updated.maxWeight,
                maxWeightReps = updated.maxWeightReps, maxReps = updated.maxReps,
                bestVolume = updated.bestVolume,
                recordDate = updated.recordDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                athleteWeight = updated.athleteWeight,
                bestVolumeWeight = updated.bestVolumeWeight, bestVolumeReps = updated.bestVolumeReps
            ))
        }
    }

    // ==================== LOAD FROM DB ====================

    fun loadArchiveFromDatabase() {
        viewModelScope.launch {
            _completedWorkouts.value = database.workoutHistoryDao().getAllWorkouts().map { e ->
                val exEntities = database.completedWorkoutExerciseDao().getByWorkout(e.id)
                val allSets    = database.completedWorkoutSetDao().getByWorkout(e.id).groupBy { it.exerciseName }
                CompletedWorkout(
                    id = e.id,
                    dateTime = Instant.ofEpochMilli(e.date).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    athleteWeight = e.athleteWeight, muscleGroup = e.muscleGroup,
                    durationMinutes = e.duration ?: 0,
                    exercises = exEntities.map { ex ->
                        WorkoutExercise(name = ex.exerciseName,
                            sets = (allSets[ex.exerciseName] ?: emptyList()).map { WorkoutSet(it.weight, it.reps) })
                    }
                )
            }
        }
    }

    fun loadRecordsFromDatabase() {
        viewModelScope.launch {
            _exerciseHistory.value = database.exerciseHistoryDao().getAll()
                .associateBy { it.exerciseName }
                .mapValues { (_, e) -> ExerciseHistory(
                    exerciseName = e.exerciseName, maxWeight = e.maxWeight, maxWeightReps = e.maxWeightReps,
                    maxReps = e.maxReps, bestVolume = e.bestVolume,
                    recordDate = e.recordDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() },
                    athleteWeight = e.athleteWeight, bestVolumeWeight = e.bestVolumeWeight, bestVolumeReps = e.bestVolumeReps
                )}
        }
    }

    fun loadPlannedWorkouts() {
        viewModelScope.launch {
            _plannedWorkouts.value = database.plannedWorkoutDao().getAll().map { e ->
                PlannedWorkout(
                    id = e.id, date = LocalDate.ofEpochDay(e.date),
                    hour = e.hour, minute = e.minute, muscleGroup = e.muscleGroup,
                    exercises = database.plannedWorkoutDao().getExercises(e.id).map { it.exerciseName }
                )
            }
        }
    }

    fun addPlannedWorkout(date: LocalDate, hour: Int, minute: Int, muscleGroup: String, exercises: List<String>) {
        viewModelScope.launch {
            val id = database.plannedWorkoutDao().insert(
                PlannedWorkoutEntity(date = date.toEpochDay(), hour = hour, minute = minute, muscleGroup = muscleGroup)
            )
            exercises.forEach { database.plannedWorkoutDao().insertExercise(PlannedExerciseEntity(workoutId = id, exerciseName = it)) }
            loadPlannedWorkouts()
        }
    }

    fun deletePlannedWorkout(id: Long) {
        viewModelScope.launch {
            database.plannedWorkoutDao().deleteExercisesByWorkout(id)
            database.plannedWorkoutDao().deleteById(id)
            loadPlannedWorkouts()
        }
    }

    // ==================== СВОИ УПРАЖНЕНИЯ ====================

    fun loadCustomExercises() {
        viewModelScope.launch {
            _customExercises.value = database.customExerciseDao().getAll().mapNotNull { e ->
                val group = MuscleGroup.entries.find { it.name == e.muscleGroup } ?: return@mapNotNull null
                CustomExercise(id = e.id, name = e.name, muscleGroup = group)
            }
        }
    }

    fun addCustomExercise(name: String, group: MuscleGroup) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val alreadyExists = _customExercises.value.any {
            it.muscleGroup == group && it.name.equals(trimmed, ignoreCase = true)
        }
        val existsInLibrary = ExerciseLibrary.exercises.any {
            it.muscleGroup == group && it.name.equals(trimmed, ignoreCase = true)
        }
        if (alreadyExists || existsInLibrary) return

        viewModelScope.launch {
            database.customExerciseDao().insert(CustomExerciseEntity(name = trimmed, muscleGroup = group.name))
            loadCustomExercises()
        }
    }

    // ==================== GETTERS ====================

    fun getDaysSinceLastWorkout(): Long? {
        val w = _completedWorkouts.value; if (w.isEmpty()) return null
        return Duration.between(w.last().dateTime, LocalDateTime.now()).toDays()
    }
    fun getWeightDifference(): Float? {
        val w = _completedWorkouts.value; if (w.size < 2) return null
        return (w.last().athleteWeight ?: return null) - (w[w.size-2].athleteWeight ?: return null)
    }
    fun isExerciseRecord(name: String, weight: Float, reps: Int): Boolean {
        val r = _exerciseHistory.value[name] ?: return false
        return weight >= r.maxWeight && reps >= r.maxWeightReps
    }
    fun getPreviousSet(name: String, index: Int): WorkoutSet? {
        for (w in _completedWorkouts.value.asReversed()) {
            val ex = w.exercises.find { it.name == name }
            if (ex != null && ex.sets.size > index) return ex.sets[index]
        }; return null
    }
    fun getCurrentExercisesCount() = _state.value.selectedExercises.size
    fun getCurrentSetsCount()      = _state.value.selectedExercises.sumOf { it.sets.size }
    fun getCurrentVolume()         = _state.value.selectedExercises.sumOf { ex -> ex.sets.sumOf { (it.weight * it.reps).toDouble() } }.toFloat()
}