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
import com.vkasport.app.notifications.ReminderScheduler
import com.vkasport.app.data.backup.BackupManager
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrainingSessionViewModel(private val database: WorkoutDatabase) : ViewModel() {

    private val _state            = MutableStateFlow(CurrentWorkoutState())
    private val _completedWorkouts= MutableStateFlow<List<CompletedWorkout>>(emptyList())
    private val _exerciseHistory  = MutableStateFlow<Map<String, ExerciseHistory>>(emptyMap())
    private val _plannedWorkouts  = MutableStateFlow<List<PlannedWorkout>>(emptyList())
    private val _customExercises  = MutableStateFlow<List<CustomExercise>>(emptyList())
    private val _lastCompletedWorkoutId = MutableStateFlow<Long?>(null)

    // ТАЙМЕР ОТДЫХА: момент последней записи подхода (epoch millis),
    // null = таймер не идёт. Хранится ТОЛЬКО в памяти (не в БД, схему
    // не трогаем) — после перезапуска приложения таймер запустится
    // заново при следующем подходе, это осознанный компромисс.
    private val _restTimerStart = MutableStateFlow<Long?>(null)

    // ЖУРНАЛ ТЕЛА (этап «тело»): записи веса и замеров, отсортированы по
    // дате ПО ВОЗРАСТАНИЮ (старые первые — так удобнее строить графики)
    private val _bodyMetrics = MutableStateFlow<List<BodyMetricEntity>>(emptyList())

    val state: StateFlow<CurrentWorkoutState>     = _state
    val completedWorkouts = _completedWorkouts.asStateFlow()
    val exerciseHistory   = _exerciseHistory.asStateFlow()
    val plannedWorkouts   = _plannedWorkouts.asStateFlow()
    val customExercises   = _customExercises.asStateFlow()
    val lastCompletedWorkoutId = _lastCompletedWorkoutId.asStateFlow()
    val restTimerStart = _restTimerStart.asStateFlow()
    val bodyMetrics = _bodyMetrics.asStateFlow()

    // ВАЖНЫЙ ИНВАРИАНТ: _completedWorkouts ВСЕГДА отсортирован по dateTime
    // по убыванию — [0] самая свежая тренировка, последняя — самая старая.
    // Это гарантируется этой функцией — используйте её при любом изменении
    // списка вместо прямого присваивания _completedWorkouts.value.
    private fun setCompletedWorkouts(list: List<CompletedWorkout>) {
        _completedWorkouts.value = list.sortedByDescending { it.dateTime }
    }

    // ==================== ЦЕНТРАЛЬНОЕ ИЗМЕНЕНИЕ СОСТОЯНИЯ ====================

    private fun setState(newState: CurrentWorkoutState) {
        _state.value = newState
        persistInProgressWorkout(newState)
    }

    private fun persistInProgressWorkout(s: CurrentWorkoutState) {
        viewModelScope.launch {
            if (!s.trainingStarted || s.currentScreen == "start" || s.currentScreen == "summary") {
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

    private fun exercisesToJson(exercises: List<WorkoutExercise>): String {
        val arr = JSONArray()
        exercises.forEach { ex ->
            val obj = JSONObject()
            obj.put("name", ex.name)
            obj.put("muscleGroup", ex.muscleGroup?.name ?: JSONObject.NULL)
            obj.put("measureType", ex.measureType.name)
            val setsArr = JSONArray()
            ex.sets.forEach { s ->
                val sObj = JSONObject()
                sObj.put("weight", s.weight.toDouble())
                sObj.put("reps", s.reps)
                s.seconds?.let { sObj.put("seconds", it) }
                s.distanceKm?.let { sObj.put("distanceKm", it.toDouble()) }
                s.load?.let { sObj.put("load", it.toDouble()) }
                s.speed?.let { sObj.put("speed", it.toDouble()) }
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
                val measureType = obj.optString("measureType", "")
                    .let { s -> MeasureType.entries.find { it.name == s } } ?: MeasureType.WEIGHT_REPS
                val setsArr = obj.getJSONArray("sets")
                val sets = mutableListOf<WorkoutSet>()
                for (j in 0 until setsArr.length()) {
                    val sObj = setsArr.getJSONObject(j)
                    sets.add(WorkoutSet(
                        weight = sObj.getDouble("weight").toFloat(),
                        reps = sObj.getInt("reps"),
                        seconds = if (sObj.has("seconds")) sObj.getInt("seconds") else null,
                        distanceKm = if (sObj.has("distanceKm")) sObj.getDouble("distanceKm").toFloat() else null,
                        load = if (sObj.has("load")) sObj.getDouble("load").toFloat() else null,
                        speed = if (sObj.has("speed")) sObj.getDouble("speed").toFloat() else null
                    ))
                }
                // id НЕ сохраняем/восстанавливаем — при восстановлении
                // каждый WorkoutExercise получает свежий UUID автоматически,
                // это нормально: id важен только для UI-ключей в рамках
                // текущей композиции, не для истории
                list.add(WorkoutExercise(name = name, muscleGroup = group, sets = sets, measureType = measureType))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== WORKOUT ACTIONS ====================

    fun selectMuscleGroup(group: MuscleGroup) { setState(_state.value.copy(selectedMuscleGroup = group)) }

    // ИСПРАВЛЕНО: разрешено добавлять одно и то же упражнение (по имени)
    // несколько раз за тренировку — например жим лёжа → другое упражнение →
    // снова жим лёжа. Крашей не будет, т.к. у каждого экземпляра свой
    // уникальный id (см. WorkoutExercise), используемый как ключ в списке.
    fun addExercise(name: String, measureTypeOverride: MeasureType? = null) {
        val group = _state.value.selectedMuscleGroup ?: return
        val exercise = WorkoutExercise(
            name = name,
            muscleGroup = group,
            // override нужен для только что созданного своего упражнения:
            // его тип может ещё не успеть загрузиться из БД в customExercises
            measureType = measureTypeOverride ?: resolveMeasureType(name, group)
        )
        setState(_state.value.copy(selectedExercises = _state.value.selectedExercises + exercise))
    }

    /**
     * Как считается упражнение с таким именем: ищем в библиотеке, потом в
     * своих упражнениях. Не нашли — классика WEIGHT_REPS.
     */
    private fun resolveMeasureType(name: String, group: MuscleGroup?): MeasureType {
        ExerciseLibrary.exercises.find {
            it.name.equals(name, ignoreCase = true) && (group == null || it.muscleGroup == group)
        }?.let { return it.measureType }
        _customExercises.value.find {
            it.name.equals(name, ignoreCase = true) && (group == null || it.muscleGroup == group)
        }?.let { return it.measureType }
        return MeasureType.WEIGHT_REPS
    }

    // Убрать КОНКРЕТНЫЙ экземпляр упражнения (по id, не по имени — иначе
    // при дублях удалились бы сразу все одноимённые карточки)
    fun removeExercise(exerciseId: String) {
        setState(_state.value.copy(
            selectedExercises = _state.value.selectedExercises.filterNot { it.id == exerciseId }
        ))
    }

    fun removeMuscleGroup(group: MuscleGroup) {
        setState(_state.value.copy(
            selectedExercises = _state.value.selectedExercises.filterNot { it.muscleGroup == group }
        ))
    }

    fun updateAthleteWeight(w: Float) { setState(_state.value.copy(athleteWeight = w)) }
    fun startTraining() { setState(_state.value.copy(trainingStarted = true, currentScreen = "weight")) }
    fun setCurrentScreen(s: String) {
        // Запускаем таймер отдыха при первом входе на экран тренировки,
        // чтобы он показывал время ещё до первого записанного подхода
        if (s == "training" && _restTimerStart.value == null) {
            _restTimerStart.value = System.currentTimeMillis()
        }
        setState(_state.value.copy(currentScreen = s))
    }

    // Старый вариант (вес×повторы) — делегирует общему, сохранён для
    // совместимости вызовов
    fun addSetToExercise(exerciseId: String, weight: Float, reps: Int) =
        addSetToExercise(exerciseId, WorkoutSet(weight, reps))

    // МОДЕЛЬ V2: добавить подход ЛЮБОГО типа конкретному экземпляру (по id)
    fun addSetToExercise(exerciseId: String, set: WorkoutSet) {
        val target = _state.value.selectedExercises.find { it.id == exerciseId } ?: return
        val updated = _state.value.selectedExercises.map { ex ->
            if (ex.id == exerciseId) ex.copy(sets = ex.sets + set) else ex
        }
        setState(_state.value.copy(selectedExercises = updated))
        updateStatsForSet(target, set)
        // Записан новый подход — таймер отдыха начинает отсчёт заново
        _restTimerStart.value = System.currentTimeMillis()
    }

    // Редактирование уже введённого подхода — тоже по id экземпляра,
    // чтобы при дублях правился только тот конкретный подход в той
    // конкретной карточке, по которой тапнули
    fun updateSet(exerciseId: String, index: Int, weight: Float, reps: Int) =
        updateSet(exerciseId, index, WorkoutSet(weight, reps))

    // МОДЕЛЬ V2: редактирование подхода любого типа
    fun updateSet(exerciseId: String, index: Int, set: WorkoutSet) {
        val updated = _state.value.selectedExercises.map { ex ->
            if (ex.id == exerciseId && index in ex.sets.indices) {
                val newSets = ex.sets.toMutableList()
                newSets[index] = set
                ex.copy(sets = newSets)
            } else ex
        }
        setState(_state.value.copy(selectedExercises = updated))
    }

    fun resetWorkout() {
        _restTimerStart.value = null
        setState(CurrentWorkoutState())
        _lastCompletedWorkoutId.value = null
        loadArchiveFromDatabase()
        loadRecordsFromDatabase()
    }

    // ==================== ПОВТОР ГРУППЫ ИЗ ПРОШЛОЙ ТРЕНИРОВКИ ====================
    // _completedWorkouts гарантированно отсортирован от новых к старым,
    // поэтому просто идём по списку СНАЧАЛА (без .asReversed() — раньше
    // это было ошибкой и из-за неё показывались данные не из последней
    // тренировки, а из более старой)

    fun getLastMuscleGroupExercises(group: MuscleGroup): List<String> {
        val last = _completedWorkouts.value
            .firstOrNull { workout -> workout.exercises.any { it.muscleGroup == group } }
            ?: return emptyList()
        return last.exercises.filter { it.muscleGroup == group }.map { it.name }
    }

    fun repeatLastMuscleGroup(group: MuscleGroup) {
        val lastWithGroup = _completedWorkouts.value
            .firstOrNull { workout -> workout.exercises.any { it.muscleGroup == group } }
            ?: return

        val alreadyAdded = _state.value.selectedExercises.map { it.name }.toSet()
        val toAdd = lastWithGroup.exercises
            .filter { it.muscleGroup == group && it.name !in alreadyAdded }
            .map { WorkoutExercise(name = it.name, muscleGroup = group, measureType = it.measureType) }

        if (toAdd.isEmpty()) return
        setState(_state.value.copy(selectedExercises = _state.value.selectedExercises + toAdd))
    }

    // ==================== FINISH WORKOUT ====================

    fun finishCurrentWorkout() {
        val s = _state.value
        // ИСПРАВЛЕНО: фиксируем момент ЗАВЕРШЕНИЯ один раз и используем его
        // и для БД, и для памяти — раньше в памяти использовалось время
        // НАЧАЛА тренировки (s.workoutStartTime), а в БД — момент
        // завершения, из-за чего сортировка и "последняя тренировка"
        // работали по-разному до и после перезагрузки архива из БД.
        val finishTime = LocalDateTime.now()
        val durationMinutes = Duration.between(s.workoutStartTime, finishTime).toMinutes()

        val usedGroups = s.selectedExercises.mapNotNull { it.muscleGroup }.distinct()
        val muscleGroupSummary =
            if (usedGroups.isNotEmpty()) usedGroups.joinToString(", ") { it.title }
            else s.selectedMuscleGroup?.title ?: "Не выбрано"

        viewModelScope.launch {
            val wid = database.workoutHistoryDao().insertWorkout(
                CompletedWorkoutEntity(
                    date = finishTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    duration = durationMinutes,
                    athleteWeight = s.athleteWeight,
                    muscleGroup = muscleGroupSummary
                )
            )
            s.selectedExercises.forEach { ex ->
                database.completedWorkoutExerciseDao().insert(
                    CompletedWorkoutExerciseEntity(
                        workoutId = wid,
                        exerciseName = ex.name,
                        muscleGroup = ex.muscleGroup?.name,
                        measureType = ex.measureType.name
                    )
                )
                ex.sets.forEach { set -> database.completedWorkoutSetDao().insert(CompletedWorkoutSetEntity(
                    workoutId = wid, exerciseName = ex.name,
                    weight = set.weight, reps = set.reps,
                    seconds = set.seconds, distanceKm = set.distanceKm,
                    load = set.load, speed = set.speed
                )) }
            }
            database.inProgressWorkoutDao().clear()
            _lastCompletedWorkoutId.value = wid
        }

        setCompletedWorkouts(_completedWorkouts.value + CompletedWorkout(
            dateTime = finishTime,
            athleteWeight = s.athleteWeight,
            muscleGroup = muscleGroupSummary,
            exercises = s.selectedExercises,
            durationMinutes = durationMinutes
        ))
        _restTimerStart.value = null
        _state.value = _state.value.copy(currentScreen = "summary")
    }

    fun saveWorkoutNotes(notes: String) {
        val wid = _lastCompletedWorkoutId.value ?: return
        val trimmed = notes.trim().ifBlank { null }
        viewModelScope.launch {
            database.workoutHistoryDao().updateNotes(wid, trimmed)
        }
        setCompletedWorkouts(_completedWorkouts.value.map {
            if (it.id == wid) it.copy(notes = trimmed) else it
        })
    }

    // ==================== УДАЛЕНИЕ ТРЕНИРОВКИ ИЗ АРХИВА ====================

    fun deleteWorkout(id: Long) {
        viewModelScope.launch {
            database.completedWorkoutExerciseDao().deleteByWorkout(id)
            database.completedWorkoutSetDao().deleteByWorkout(id)
            database.workoutHistoryDao().deleteById(id)
        }
        setCompletedWorkouts(_completedWorkouts.value.filterNot { it.id == id })
    }

    // ==================== EXERCISE STATS ====================

    /**
     * Обновление рекордов с учётом типа упражнения:
     *  WEIGHT_REPS — классическая логика (макс. вес / объём)
     *  REPS        — макс. повторы
     *  TIME        — лучшее (наибольшее) время
     *  DISTANCE    — макс. дистанция
     *  CARDIO      — рекорды не ведутся
     */
    private fun updateStatsForSet(target: WorkoutExercise, set: WorkoutSet) {
        if (target.measureType == MeasureType.WEIGHT_REPS) {
            updateExerciseStats(target.name, set.weight, set.reps)
            return
        }
        if (target.measureType == MeasureType.CARDIO) return

        val cur = _exerciseHistory.value[target.name]
        val isNewRecord = when (target.measureType) {
            MeasureType.REPS     -> set.reps > (cur?.maxReps ?: 0)
            MeasureType.TIME     -> (set.seconds ?: 0) > (cur?.bestSeconds ?: 0)
            MeasureType.DISTANCE -> (set.distanceKm ?: 0f) > (cur?.bestDistanceKm ?: 0f)
            else -> false
        }
        if (!isNewRecord) return

        val updated = (cur ?: ExerciseHistory(exerciseName = target.name)).copy(
            measureType = target.measureType,
            maxReps = if (target.measureType == MeasureType.REPS) set.reps else cur?.maxReps ?: 0,
            bestSeconds = if (target.measureType == MeasureType.TIME) set.seconds else cur?.bestSeconds,
            bestDistanceKm = if (target.measureType == MeasureType.DISTANCE) set.distanceKm else cur?.bestDistanceKm,
            recordDate = LocalDateTime.now(),
            athleteWeight = _state.value.athleteWeight
        )
        _exerciseHistory.value = _exerciseHistory.value + (target.name to updated)
        viewModelScope.launch {
            database.exerciseHistoryDao().save(ExerciseHistoryEntity(
                exerciseName = updated.exerciseName, maxWeight = updated.maxWeight,
                maxWeightReps = updated.maxWeightReps, maxReps = updated.maxReps,
                bestVolume = updated.bestVolume,
                recordDate = updated.recordDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                athleteWeight = updated.athleteWeight,
                bestVolumeWeight = updated.bestVolumeWeight, bestVolumeReps = updated.bestVolumeReps,
                measureType = updated.measureType.name,
                bestSeconds = updated.bestSeconds, bestDistanceKm = updated.bestDistanceKm
            ))
        }
    }

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
                bestVolumeWeight = updated.bestVolumeWeight, bestVolumeReps = updated.bestVolumeReps,
                measureType = updated.measureType.name,
                bestSeconds = updated.bestSeconds, bestDistanceKm = updated.bestDistanceKm
            ))
        }
    }

    // ==================== LOAD FROM DB ====================

    fun loadArchiveFromDatabase() {
        viewModelScope.launch {
            val loaded = database.workoutHistoryDao().getAllWorkouts().map { e ->
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
                            measureType = ex.measureType?.let { mt -> MeasureType.entries.find { it.name == mt } }
                                ?: MeasureType.WEIGHT_REPS,
                            sets = (allSets[ex.exerciseName] ?: emptyList()).map {
                                WorkoutSet(it.weight, it.reps, it.seconds, it.distanceKm, it.load, it.speed)
                            }
                        )
                    }
                )
            }
            setCompletedWorkouts(loaded)
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
                    athleteWeight = e.athleteWeight, bestVolumeWeight = e.bestVolumeWeight, bestVolumeReps = e.bestVolumeReps,
                    measureType = e.measureType?.let { mt -> MeasureType.entries.find { it.name == mt } }
                        ?: MeasureType.WEIGHT_REPS,
                    bestSeconds = e.bestSeconds, bestDistanceKm = e.bestDistanceKm
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
            // Ставим напоминания: утром 9:00 в день тренировки + за 2 часа
            ReminderScheduler.schedulePlanned(id, date, hour, minute, muscleGroup)
            loadPlannedWorkouts()
        }
    }

    fun deletePlannedWorkout(id: Long) {
        viewModelScope.launch {
            database.plannedWorkoutDao().deleteExercisesByWorkout(id)
            database.plannedWorkoutDao().deleteById(id)
            // Снимаем оба напоминания этой тренировки
            ReminderScheduler.cancelPlanned(id)
            loadPlannedWorkouts()
        }
    }

    // ==================== СВОИ УПРАЖНЕНИЯ ====================

    fun loadCustomExercises() {
        viewModelScope.launch {
            _customExercises.value = database.customExerciseDao().getAll().mapNotNull { e ->
                val group = MuscleGroup.entries.find { it.name == e.muscleGroup } ?: return@mapNotNull null
                CustomExercise(
                    id = e.id, name = e.name, muscleGroup = group,
                    measureType = e.measureType?.let { mt -> MeasureType.entries.find { it.name == mt } }
                        ?: MeasureType.WEIGHT_REPS
                )
            }
        }
    }

    fun addCustomExercise(name: String, group: MuscleGroup, measureType: MeasureType = MeasureType.WEIGHT_REPS) {
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
            database.customExerciseDao().insert(CustomExerciseEntity(
                name = trimmed, muscleGroup = group.name, measureType = measureType.name
            ))
            loadCustomExercises()
        }
    }

    // ==================== GETTERS ====================
    // _completedWorkouts гарантированно DESC (новые первые) — см.
    // setCompletedWorkouts(). Поэтому first() = самая свежая тренировка.

    fun getDaysSinceLastWorkout(): Long? {
        val w = _completedWorkouts.value; if (w.isEmpty()) return null
        return Duration.between(w.first().dateTime, LocalDateTime.now()).toDays()
    }
    fun getWeightDifference(): Float? {
        val w = _completedWorkouts.value; if (w.size < 2) return null
        return (w[0].athleteWeight ?: return null) - (w[1].athleteWeight ?: return null)
    }
    fun isExerciseRecord(name: String, weight: Float, reps: Int): Boolean {
        val r = _exerciseHistory.value[name] ?: return false
        return weight >= r.maxWeight && reps >= r.maxWeightReps
    }
    // ИСПРАВЛЕНО: раньше здесь был .asReversed(), что при DESC-порядке
    // списка означало перебор от САМОЙ СТАРОЙ тренировки к новой — отсюда
    // "прошлая тренировка" в карточке упражнения показывала не последние,
    // а устаревшие данные. Теперь идём по списку как есть (новые первыми).
    fun getPreviousSet(name: String, index: Int): WorkoutSet? {
        for (w in _completedWorkouts.value) {
            val ex = w.exercises.find { it.name == name }
            if (ex != null && ex.sets.size > index) return ex.sets[index]
        }
        return null
    }
    fun getCurrentExercisesCount() = _state.value.selectedExercises.size
    fun getCurrentSetsCount()      = _state.value.selectedExercises.sumOf { it.sets.size }
    fun getCurrentVolume()         = _state.value.selectedExercises.sumOf { ex -> ex.sets.sumOf { (it.weight * it.reps).toDouble() } }.toFloat()

    // ==================== ЖУРНАЛ ТЕЛА ====================

    fun loadBodyMetrics() {
        viewModelScope.launch { _bodyMetrics.value = database.bodyMetricDao().getAll() }
    }

    fun addBodyMetric(entity: BodyMetricEntity) {
        viewModelScope.launch {
            database.bodyMetricDao().insert(entity)
            loadBodyMetrics()
        }
    }

    fun deleteBodyMetric(id: Long) {
        viewModelScope.launch {
            database.bodyMetricDao().deleteById(id)
            loadBodyMetrics()
        }
    }

    /**
     * История веса для графика: объединяет вес атлета из тренировок
     * (вводится на старте каждой тренировки) и ручные записи журнала тела.
     * Несколько значений за один день усредняются. Отсортировано по дате.
     */
    fun getWeightHistory(): List<Pair<LocalDate, Float>> {
        val fromWorkouts = _completedWorkouts.value.mapNotNull { w ->
            w.athleteWeight?.let { w.dateTime.toLocalDate() to it }
        }
        val fromMetrics = _bodyMetrics.value.mapNotNull { m ->
            m.weight?.let { LocalDate.ofEpochDay(m.date) to it }
        }
        return (fromWorkouts + fromMetrics)
            .groupBy { it.first }
            .map { (d, list) -> d to (list.map { it.second }.average().toFloat()) }
            .sortedBy { it.first }
    }

    // ==================== БЭКАП / ВОССТАНОВЛЕНИЕ ====================

    /** Сохраняет все данные в файл, выбранный пользователем (SAF-uri). */
    fun exportBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = BackupManager.exportJson(database)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Не удалось открыть файл для записи")
                }
                onResult(true, "Бэкап сохранён")
            } catch (e: Exception) {
                onResult(false, "Ошибка сохранения: ${e.message}")
            }
        }
    }

    /**
     * Восстанавливает данные из файла бэкапа. ЗАМЕНЯЕТ все текущие данные
     * (архив, рекорды, план, свои упражнения, черновик тренировки).
     */
    fun importBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: throw IllegalStateException("Не удалось открыть файл")

                BackupManager.importJson(database, json)

                // Полная перезагрузка всего состояния из восстановленной БД
                _restTimerStart.value = null
                setState(CurrentWorkoutState())
                loadArchiveFromDatabase()
                loadRecordsFromDatabase()
                loadCustomExercises()
                loadPlannedWorkouts()
                loadBodyMetrics()

                onResult(true, "Данные восстановлены")
            } catch (e: IllegalArgumentException) {
                onResult(false, e.message ?: "Неверный формат файла")
            } catch (e: Exception) {
                onResult(false, "Ошибка восстановления: ${e.message}")
            }
        }
    }
}