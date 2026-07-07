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
import org.json.JSONArray
import org.json.JSONObject

class TrainingSessionViewModel(private val database: WorkoutDatabase) : ViewModel() {

    private val _state            = MutableStateFlow(CurrentWorkoutState())
    private val _completedWorkouts= MutableStateFlow<List<CompletedWorkout>>(emptyList())
    private val _exerciseHistory  = MutableStateFlow<Map<String, ExerciseHistory>>(emptyMap())
    private val _plannedWorkouts  = MutableStateFlow<List<PlannedWorkout>>(emptyList())
    private val _customExercises  = MutableStateFlow<List<CustomExercise>>(emptyList())

    // ID тренировки, только что сохранённой в БД (для добавления заметок
    // на экране итогов после finishCurrentWorkout)
    private val _lastCompletedWorkoutId = MutableStateFlow<Long?>(null)

    val state: StateFlow<CurrentWorkoutState>     = _state
    val completedWorkouts = _completedWorkouts.asStateFlow()
    val exerciseHistory   = _exerciseHistory.asStateFlow()
    val plannedWorkouts   = _plannedWorkouts.asStateFlow()
    val customExercises   = _customExercises.asStateFlow()
    val lastCompletedWorkoutId = _lastCompletedWorkoutId.asStateFlow()

    // ==================== ЦЕНТРАЛЬНОЕ ИЗМЕНЕНИЕ СОСТОЯНИЯ ====================
    // Любое изменение _state теперь идёт через эту функцию — она же сразу
    // сохраняет черновик тренировки в БД, чтобы прогресс не терялся при
    // закрытии приложения.
    private fun setState(newState: CurrentWorkoutState) {
        _state.value = newState
        persistInProgressWorkout(newState)
    }

    private fun persistInProgressWorkout(s: CurrentWorkoutState) {
        viewModelScope.launch {
            if (!s.trainingStarted || s.currentScreen == "start" || s.currentScreen == "summary") {
                // Тренировка ещё не начата или уже завершена — черновик не нужен
                database.inProgressWorkoutDao().clear()
                return@launch
            }
            database.inProgressWorkoutDao().save(
                InProgressWorkoutEntity(
                    currentScreen = s.currentScreen,
                    athleteWeight = s.athleteWeight,
                    muscleGroup = s.selectedMuscleGroup?.name,
                    workoutStartTime = s.workoutStartTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    exercisesJson = exercisesToJson(s.selectedExercises)
                )
            )
        }
    }

    // Восстанавливает черновик тренировки при запуске приложения, если он есть
    fun loadInProgressWorkout() {
        viewModelScope.launch {
            val saved = database.inProgressWorkoutDao().get() ?: return@launch
            val group = saved.muscleGroup?.let { g -> MuscleGroup.entries.find { it.name == g } }
            _state.value = CurrentWorkoutState(
                workoutStartTime = Instant.ofEpochMilli(saved.workoutStartTime).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                athleteWeight = saved.athleteWeight,
                selectedMuscleGroup = group,
                selectedExercises = jsonToExercises(saved.exercisesJson),
                trainingStarted = true,
                currentScreen = saved.currentScreen
            )
        }
    }

    // ── Простая JSON-сериализация через org.json (входит в Android SDK,
    //    доп. зависимостей не требует) ──────────────────────────────────
    private fun exercisesToJson(exercises: List<WorkoutExercise>): String {
        val arr = JSONArray()
        exercises.forEach { ex ->
            val obj = JSONObject()
            obj.put("name", ex.name)
            obj.put("muscleGroup", ex.muscleGroup?.name ?: JSONObject.NULL)
            val setsArr = JSONArray()
            ex.sets.forEach { s ->
                val sObj = JSONObject()
                sObj.put("weight", s.weight.toDouble())
                sObj.put("reps", s.reps)
                setsArr.put(sObj)
            }
            obj.put("sets", setsArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun jsonToExercises(json: String): List<WorkoutExercise> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<WorkoutExercise>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.getString("name")
                val groupStr = if (obj.isNull("muscleGroup")) null else obj.getString("muscleGroup")
                val group = groupStr?.let { s -> MuscleGroup.entries.find { it.name == s } }
                val setsArr = obj.getJSONArray("sets")
                val sets = mutableListOf<WorkoutSet>()
                for (j in 0 until setsArr.length()) {
                    val sObj = setsArr.getJSONObject(j)
                    sets.add(WorkoutSet(weight = sObj.getDouble("weight").toFloat(), reps = sObj.getInt("reps")))
                }
                list.add(WorkoutExercise(name = name, muscleGroup = group, sets = sets))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== WORKOUT ACTIONS ====================

    fun selectMuscleGroup(group: MuscleGroup) { setState(_state.value.copy(selectedMuscleGroup = group)) }

    fun addExercise(name: String) {
        val group = _state.value.selectedMuscleGroup ?: return
        // ИСПРАВЛЕНО: защита от дублей — раньше повторный тап по уже
        // добавленному упражнению создавал вторую запись с тем же именем,
        // и LazyColumn с key={"ex_${it.name}"} падал с крашем из-за
        // одинаковых ключей.
        if (_state.value.selectedExercises.any { it.name == name }) return
        val exercise = WorkoutExercise(name = name, muscleGroup = group)
        setState(_state.value.copy(selectedExercises = _state.value.selectedExercises + exercise))
    }

    fun removeExercise(name: String) {
        setState(_state.value.copy(
            selectedExercises = _state.value.selectedExercises.filterNot { it.name == name }
        ))
    }

    fun removeMuscleGroup(group: MuscleGroup) {
        setState(_state.value.copy(
            selectedExercises = _state.value.selectedExercises.filterNot { it.muscleGroup == group }
        ))
    }

    fun updateAthleteWeight(w: Float) { setState(_state.value.copy(athleteWeight = w)) }
    fun startTraining() { setState(_state.value.copy(trainingStarted = true, currentScreen = "weight")) }
    fun setCurrentScreen(s: String) { setState(_state.value.copy(currentScreen = s)) }

    fun addSetToExercise(exerciseName: String, weight: Float, reps: Int) {
        val updated = _state.value.selectedExercises.map { ex ->
            if (ex.name == exerciseName) ex.copy(sets = ex.sets + WorkoutSet(weight, reps)) else ex
        }
        setState(_state.value.copy(selectedExercises = updated))
        updateExerciseStats(exerciseName, weight, reps)
    }

    // Редактирование уже введённого подхода (по индексу) во время тренировки
    fun updateSet(exerciseName: String, index: Int, weight: Float, reps: Int) {
        val updated = _state.value.selectedExercises.map { ex ->
            if (ex.name == exerciseName && index in ex.sets.indices) {
                val newSets = ex.sets.toMutableList()
                newSets[index] = WorkoutSet(weight, reps)
                ex.copy(sets = newSets)
            } else ex
        }
        setState(_state.value.copy(selectedExercises = updated))
    }

    fun resetWorkout() {
        setState(CurrentWorkoutState())
        _lastCompletedWorkoutId.value = null
        loadArchiveFromDatabase()
        loadRecordsFromDatabase()
    }

    // ==================== ПОВТОР ГРУППЫ ИЗ ПРОШЛОЙ ТРЕНИРОВКИ ====================

    fun getLastMuscleGroupExercises(group: MuscleGroup): List<String> {
        val last = _completedWorkouts.value.asReversed()
            .firstOrNull { workout -> workout.exercises.any { it.muscleGroup == group } }
            ?: return emptyList()
        return last.exercises.filter { it.muscleGroup == group }.map { it.name }
    }

    fun repeatLastMuscleGroup(group: MuscleGroup) {
        val lastWithGroup = _completedWorkouts.value.asReversed()
            .firstOrNull { workout -> workout.exercises.any { it.muscleGroup == group } }
            ?: return

        val alreadyAdded = _state.value.selectedExercises.map { it.name }.toSet()
        val toAdd = lastWithGroup.exercises
            .filter { it.muscleGroup == group && it.name !in alreadyAdded }
            .map { WorkoutExercise(name = it.name, muscleGroup = group) }

        if (toAdd.isEmpty()) return
        setState(_state.value.copy(selectedExercises = _state.value.selectedExercises + toAdd))
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
                database.completedWorkoutExerciseDao().insert(
                    CompletedWorkoutExerciseEntity(
                        workoutId = wid,
                        exerciseName = ex.name,
                        muscleGroup = ex.muscleGroup?.name
                    )
                )
                ex.sets.forEach { set -> database.completedWorkoutSetDao().insert(CompletedWorkoutSetEntity(workoutId = wid, exerciseName = ex.name, weight = set.weight, reps = set.reps)) }
            }
            // Тренировка сохранена в БД — черновик больше не нужен
            database.inProgressWorkoutDao().clear()
            _lastCompletedWorkoutId.value = wid
        }
        _completedWorkouts.value = _completedWorkouts.value + CompletedWorkout(
            dateTime = s.workoutStartTime, athleteWeight = s.athleteWeight,
            muscleGroup = muscleGroupSummary,
            exercises = s.selectedExercises,
            durationMinutes = Duration.between(s.workoutStartTime, LocalDateTime.now()).toMinutes()
        )
        // currentScreen меняем напрямую (не через setState) — черновик всё
        // равно чистится выше, лишний вызов persist не нужен
        _state.value = _state.value.copy(currentScreen = "summary")
    }

    // Сохранить заметки к только что завершённой тренировке (экран итогов)
    fun saveWorkoutNotes(notes: String) {
        val wid = _lastCompletedWorkoutId.value ?: return
        val trimmed = notes.trim().ifBlank { null }
        viewModelScope.launch {
            database.workoutHistoryDao().updateNotes(wid, trimmed)
        }
        _completedWorkouts.value = _completedWorkouts.value.map {
            if (it.id == wid) it.copy(notes = trimmed) else it
        }
    }

    // ==================== УДАЛЕНИЕ ТРЕНИРОВКИ ИЗ АРХИВА ====================

    fun deleteWorkout(id: Long) {
        viewModelScope.launch {
            database.completedWorkoutExerciseDao().deleteByWorkout(id)
            database.completedWorkoutSetDao().deleteByWorkout(id)
            database.workoutHistoryDao().deleteById(id)
        }
        _completedWorkouts.value = _completedWorkouts.value.filterNot { it.id == id }
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
                    notes = e.notes,
                    exercises = exEntities.map { ex ->
                        WorkoutExercise(
                            name = ex.exerciseName,
                            muscleGroup = ex.muscleGroup?.let { name -> MuscleGroup.entries.find { it.name == name } },
                            sets = (allSets[ex.exerciseName] ?: emptyList()).map { WorkoutSet(it.weight, it.reps) }
                        )
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