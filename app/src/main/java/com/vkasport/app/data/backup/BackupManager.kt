package com.vkasport.app.data.backup

import com.vkasport.app.data.local.database.WorkoutDatabase
import com.vkasport.app.data.local.entity.CompletedWorkoutEntity
import com.vkasport.app.data.local.entity.CompletedWorkoutExerciseEntity
import com.vkasport.app.data.local.entity.CompletedWorkoutSetEntity
import com.vkasport.app.data.local.entity.CustomExerciseEntity
import com.vkasport.app.data.local.entity.ExerciseHistoryEntity
import com.vkasport.app.data.local.entity.PlannedExerciseEntity
import com.vkasport.app.data.local.entity.PlannedWorkoutEntity
import com.vkasport.app.data.local.entity.BodyMetricEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Экспорт/импорт всех данных приложения в JSON.
 *
 * ПРАВИЛА ВЕРСИОНИРОВАНИЯ ФОРМАТА:
 *  - formatVersion 1: исходный формат (вес × повторы)
 *  - formatVersion 3: + журнал тела (bodyMetrics)
 *  - formatVersion 2: + measureType у упражнений/своих упражнений,
 *    + seconds/distanceKm/load/speed у подходов,
 *    + measureType/bestSeconds/bestDistanceKm у рекордов
 *  - Парсер КАЖДОЙ старой версии остаётся навсегда — старые бэкапы
 *    обязаны восстанавливаться всегда.
 *  - Бэкап более новой версии, чем знает приложение, не импортируем.
 */
object BackupManager {

    const val FORMAT_VERSION = 3

    // ==================== ЭКСПОРТ (всегда в новейшем формате) ====================

    suspend fun exportJson(db: WorkoutDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "VKA_SPORT")
        root.put("formatVersion", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        // --- завершённые тренировки (с вложенными упражнениями и подходами) ---
        val workoutsArr = JSONArray()
        db.workoutHistoryDao().getAllWorkouts().forEach { w ->
            val wObj = JSONObject()
            wObj.put("date", w.date)
            w.duration?.let { wObj.put("duration", it) }
            w.athleteWeight?.let { wObj.put("athleteWeight", it.toDouble()) }
            wObj.put("muscleGroup", w.muscleGroup)
            w.notes?.let { wObj.put("notes", it) }

            val exArr = JSONArray()
            db.completedWorkoutExerciseDao().getByWorkout(w.id).forEach { ex ->
                val exObj = JSONObject()
                exObj.put("name", ex.exerciseName)
                ex.muscleGroup?.let { exObj.put("muscleGroup", it) }
                ex.measureType?.let { exObj.put("measureType", it) }
                exArr.put(exObj)
            }
            wObj.put("exercises", exArr)

            val setArr = JSONArray()
            db.completedWorkoutSetDao().getByWorkout(w.id).forEach { s ->
                val sObj = JSONObject()
                sObj.put("exerciseName", s.exerciseName)
                sObj.put("weight", s.weight.toDouble())
                sObj.put("reps", s.reps)
                s.seconds?.let { sObj.put("seconds", it) }
                s.distanceKm?.let { sObj.put("distanceKm", it.toDouble()) }
                s.load?.let { sObj.put("load", it.toDouble()) }
                s.speed?.let { sObj.put("speed", it.toDouble()) }
                setArr.put(sObj)
            }
            wObj.put("sets", setArr)

            workoutsArr.put(wObj)
        }
        root.put("workouts", workoutsArr)

        // --- рекорды ---
        val recArr = JSONArray()
        db.exerciseHistoryDao().getAll().forEach { r ->
            val rObj = JSONObject()
            rObj.put("exerciseName", r.exerciseName)
            rObj.put("maxWeight", r.maxWeight.toDouble())
            rObj.put("maxWeightReps", r.maxWeightReps)
            rObj.put("maxReps", r.maxReps)
            rObj.put("bestVolume", r.bestVolume.toDouble())
            r.recordDate?.let { rObj.put("recordDate", it) }
            r.athleteWeight?.let { rObj.put("athleteWeight", it.toDouble()) }
            r.bestVolumeWeight?.let { rObj.put("bestVolumeWeight", it.toDouble()) }
            r.bestVolumeReps?.let { rObj.put("bestVolumeReps", it) }
            r.measureType?.let { rObj.put("measureType", it) }
            r.bestSeconds?.let { rObj.put("bestSeconds", it) }
            r.bestDistanceKm?.let { rObj.put("bestDistanceKm", it.toDouble()) }
            recArr.put(rObj)
        }
        root.put("records", recArr)

        // --- запланированные тренировки ---
        val plannedArr = JSONArray()
        db.plannedWorkoutDao().getAll().forEach { p ->
            val pObj = JSONObject()
            pObj.put("date", p.date)
            pObj.put("hour", p.hour)
            pObj.put("minute", p.minute)
            pObj.put("muscleGroup", p.muscleGroup)
            val pExArr = JSONArray()
            db.plannedWorkoutDao().getExercises(p.id).forEach { pExArr.put(it.exerciseName) }
            pObj.put("exercises", pExArr)
            plannedArr.put(pObj)
        }
        root.put("planned", plannedArr)

        // --- свои упражнения ---
        val customArr = JSONArray()
        db.customExerciseDao().getAll().forEach { c ->
            val cObj = JSONObject()
            cObj.put("name", c.name)
            cObj.put("muscleGroup", c.muscleGroup)
            c.measureType?.let { cObj.put("measureType", it) }
            customArr.put(cObj)
        }
        root.put("customExercises", customArr)

        // --- журнал тела ---
        val bodyArr = JSONArray()
        db.bodyMetricDao().getAll().forEach { m ->
            val mObj = JSONObject()
            mObj.put("date", m.date)
            m.weight?.let { mObj.put("weight", it.toDouble()) }
            m.chest?.let { mObj.put("chest", it.toDouble()) }
            m.waist?.let { mObj.put("waist", it.toDouble()) }
            m.hips?.let { mObj.put("hips", it.toDouble()) }
            m.biceps?.let { mObj.put("biceps", it.toDouble()) }
            m.forearm?.let { mObj.put("forearm", it.toDouble()) }
            m.thigh?.let { mObj.put("thigh", it.toDouble()) }
            m.calf?.let { mObj.put("calf", it.toDouble()) }
            m.neck?.let { mObj.put("neck", it.toDouble()) }
            m.shoulders?.let { mObj.put("shoulders", it.toDouble()) }
            bodyArr.put(mObj)
        }
        root.put("bodyMetrics", bodyArr)

        root.toString(2)
    }

    // ==================== ИМПОРТ ====================

    suspend fun importJson(db: WorkoutDatabase, json: String) = withContext(Dispatchers.IO) {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Файл не является бэкапом VKA SPORT")
        }

        if (root.optString("app") != "VKA_SPORT") {
            throw IllegalArgumentException("Файл не является бэкапом VKA SPORT")
        }

        when (val v = root.optInt("formatVersion", -1)) {
            1 -> importV1(db, root)
            2 -> importV2(db, root)
            3 -> importV3(db, root)
            else -> throw IllegalArgumentException(
                "Бэкап формата версии $v — эта версия приложения его не знает. Обновите приложение."
            )
        }
    }

    /**
     * Парсер формата v3 (текущий): всё из v2 + журнал тела.
     * v2-часть переиспользуется напрямую — она читает только известные ей
     * поля, а bodyMetrics докидываем отдельно (clearAllTables уже внутри).
     */
    private suspend fun importV3(db: WorkoutDatabase, root: JSONObject) {
        importV2(db, root)

        val bodyArr = root.optJSONArray("bodyMetrics") ?: JSONArray()
        for (i in 0 until bodyArr.length()) {
            val mObj = bodyArr.getJSONObject(i)
            db.bodyMetricDao().insert(
                BodyMetricEntity(
                    date = mObj.getLong("date"),
                    weight = if (mObj.has("weight")) mObj.getDouble("weight").toFloat() else null,
                    chest = if (mObj.has("chest")) mObj.getDouble("chest").toFloat() else null,
                    waist = if (mObj.has("waist")) mObj.getDouble("waist").toFloat() else null,
                    hips = if (mObj.has("hips")) mObj.getDouble("hips").toFloat() else null,
                    biceps = if (mObj.has("biceps")) mObj.getDouble("biceps").toFloat() else null,
                    forearm = if (mObj.has("forearm")) mObj.getDouble("forearm").toFloat() else null,
                    thigh = if (mObj.has("thigh")) mObj.getDouble("thigh").toFloat() else null,
                    calf = if (mObj.has("calf")) mObj.getDouble("calf").toFloat() else null,
                    neck = if (mObj.has("neck")) mObj.getDouble("neck").toFloat() else null,
                    shoulders = if (mObj.has("shoulders")) mObj.getDouble("shoulders").toFloat() else null
                )
            )
        }
    }

    /**
     * Парсер формата v2. ЗАМОРОЖЕН — не редактировать (используется и как
     * основа importV3: читает только свои поля).
     */
    private suspend fun importV2(db: WorkoutDatabase, root: JSONObject) {
        db.clearAllTables()

        val workoutsArr = root.optJSONArray("workouts") ?: JSONArray()
        for (i in 0 until workoutsArr.length()) {
            val wObj = workoutsArr.getJSONObject(i)
            val newId = db.workoutHistoryDao().insertWorkout(
                CompletedWorkoutEntity(
                    date = wObj.getLong("date"),
                    duration = if (wObj.has("duration")) wObj.getLong("duration") else null,
                    athleteWeight = if (wObj.has("athleteWeight")) wObj.getDouble("athleteWeight").toFloat() else null,
                    muscleGroup = wObj.getString("muscleGroup"),
                    notes = if (wObj.has("notes")) wObj.getString("notes") else null
                )
            )
            val exArr = wObj.optJSONArray("exercises") ?: JSONArray()
            for (j in 0 until exArr.length()) {
                val exObj = exArr.getJSONObject(j)
                db.completedWorkoutExerciseDao().insert(
                    CompletedWorkoutExerciseEntity(
                        workoutId = newId,
                        exerciseName = exObj.getString("name"),
                        muscleGroup = if (exObj.has("muscleGroup")) exObj.getString("muscleGroup") else null,
                        measureType = if (exObj.has("measureType")) exObj.getString("measureType") else null
                    )
                )
            }
            val setArr = wObj.optJSONArray("sets") ?: JSONArray()
            for (j in 0 until setArr.length()) {
                val sObj = setArr.getJSONObject(j)
                db.completedWorkoutSetDao().insert(
                    CompletedWorkoutSetEntity(
                        workoutId = newId,
                        exerciseName = sObj.getString("exerciseName"),
                        weight = sObj.getDouble("weight").toFloat(),
                        reps = sObj.getInt("reps"),
                        seconds = if (sObj.has("seconds")) sObj.getInt("seconds") else null,
                        distanceKm = if (sObj.has("distanceKm")) sObj.getDouble("distanceKm").toFloat() else null,
                        load = if (sObj.has("load")) sObj.getDouble("load").toFloat() else null,
                        speed = if (sObj.has("speed")) sObj.getDouble("speed").toFloat() else null
                    )
                )
            }
        }

        val recArr = root.optJSONArray("records") ?: JSONArray()
        for (i in 0 until recArr.length()) {
            val rObj = recArr.getJSONObject(i)
            db.exerciseHistoryDao().save(
                ExerciseHistoryEntity(
                    exerciseName = rObj.getString("exerciseName"),
                    maxWeight = rObj.getDouble("maxWeight").toFloat(),
                    maxWeightReps = rObj.getInt("maxWeightReps"),
                    maxReps = rObj.getInt("maxReps"),
                    bestVolume = rObj.getDouble("bestVolume").toFloat(),
                    recordDate = if (rObj.has("recordDate")) rObj.getLong("recordDate") else null,
                    athleteWeight = if (rObj.has("athleteWeight")) rObj.getDouble("athleteWeight").toFloat() else null,
                    bestVolumeWeight = if (rObj.has("bestVolumeWeight")) rObj.getDouble("bestVolumeWeight").toFloat() else null,
                    bestVolumeReps = if (rObj.has("bestVolumeReps")) rObj.getInt("bestVolumeReps") else null,
                    measureType = if (rObj.has("measureType")) rObj.getString("measureType") else null,
                    bestSeconds = if (rObj.has("bestSeconds")) rObj.getInt("bestSeconds") else null,
                    bestDistanceKm = if (rObj.has("bestDistanceKm")) rObj.getDouble("bestDistanceKm").toFloat() else null
                )
            )
        }

        val plannedArr = root.optJSONArray("planned") ?: JSONArray()
        for (i in 0 until plannedArr.length()) {
            val pObj = plannedArr.getJSONObject(i)
            val newId = db.plannedWorkoutDao().insert(
                PlannedWorkoutEntity(
                    date = pObj.getLong("date"),
                    hour = pObj.getInt("hour"),
                    minute = pObj.getInt("minute"),
                    muscleGroup = pObj.getString("muscleGroup")
                )
            )
            val pExArr = pObj.optJSONArray("exercises") ?: JSONArray()
            for (j in 0 until pExArr.length()) {
                db.plannedWorkoutDao().insertExercise(
                    PlannedExerciseEntity(workoutId = newId, exerciseName = pExArr.getString(j))
                )
            }
        }

        val customArr = root.optJSONArray("customExercises") ?: JSONArray()
        for (i in 0 until customArr.length()) {
            val cObj = customArr.getJSONObject(i)
            db.customExerciseDao().insert(
                CustomExerciseEntity(
                    name = cObj.getString("name"),
                    muscleGroup = cObj.getString("muscleGroup"),
                    measureType = if (cObj.has("measureType")) cObj.getString("measureType") else null
                )
            )
        }
    }

    /** Парсер формата v1. ЗАМОРОЖЕН — не редактировать. */
    private suspend fun importV1(db: WorkoutDatabase, root: JSONObject) {
        // Полная очистка (штатный Room API, все таблицы, включая черновик)
        db.clearAllTables()

        // --- тренировки ---
        val workoutsArr = root.optJSONArray("workouts") ?: JSONArray()
        for (i in 0 until workoutsArr.length()) {
            val wObj = workoutsArr.getJSONObject(i)
            val newId = db.workoutHistoryDao().insertWorkout(
                CompletedWorkoutEntity(
                    date = wObj.getLong("date"),
                    duration = if (wObj.has("duration")) wObj.getLong("duration") else null,
                    athleteWeight = if (wObj.has("athleteWeight")) wObj.getDouble("athleteWeight").toFloat() else null,
                    muscleGroup = wObj.getString("muscleGroup"),
                    notes = if (wObj.has("notes")) wObj.getString("notes") else null
                )
            )
            val exArr = wObj.optJSONArray("exercises") ?: JSONArray()
            for (j in 0 until exArr.length()) {
                val exObj = exArr.getJSONObject(j)
                db.completedWorkoutExerciseDao().insert(
                    CompletedWorkoutExerciseEntity(
                        workoutId = newId,
                        exerciseName = exObj.getString("name"),
                        muscleGroup = if (exObj.has("muscleGroup")) exObj.getString("muscleGroup") else null
                    )
                )
            }
            val setArr = wObj.optJSONArray("sets") ?: JSONArray()
            for (j in 0 until setArr.length()) {
                val sObj = setArr.getJSONObject(j)
                db.completedWorkoutSetDao().insert(
                    CompletedWorkoutSetEntity(
                        workoutId = newId,
                        exerciseName = sObj.getString("exerciseName"),
                        weight = sObj.getDouble("weight").toFloat(),
                        reps = sObj.getInt("reps")
                    )
                )
            }
        }

        // --- рекорды ---
        val recArr = root.optJSONArray("records") ?: JSONArray()
        for (i in 0 until recArr.length()) {
            val rObj = recArr.getJSONObject(i)
            db.exerciseHistoryDao().save(
                ExerciseHistoryEntity(
                    exerciseName = rObj.getString("exerciseName"),
                    maxWeight = rObj.getDouble("maxWeight").toFloat(),
                    maxWeightReps = rObj.getInt("maxWeightReps"),
                    maxReps = rObj.getInt("maxReps"),
                    bestVolume = rObj.getDouble("bestVolume").toFloat(),
                    recordDate = if (rObj.has("recordDate")) rObj.getLong("recordDate") else null,
                    athleteWeight = if (rObj.has("athleteWeight")) rObj.getDouble("athleteWeight").toFloat() else null,
                    bestVolumeWeight = if (rObj.has("bestVolumeWeight")) rObj.getDouble("bestVolumeWeight").toFloat() else null,
                    bestVolumeReps = if (rObj.has("bestVolumeReps")) rObj.getInt("bestVolumeReps") else null
                )
            )
        }

        // --- запланированные ---
        val plannedArr = root.optJSONArray("planned") ?: JSONArray()
        for (i in 0 until plannedArr.length()) {
            val pObj = plannedArr.getJSONObject(i)
            val newId = db.plannedWorkoutDao().insert(
                PlannedWorkoutEntity(
                    date = pObj.getLong("date"),
                    hour = pObj.getInt("hour"),
                    minute = pObj.getInt("minute"),
                    muscleGroup = pObj.getString("muscleGroup")
                )
            )
            val pExArr = pObj.optJSONArray("exercises") ?: JSONArray()
            for (j in 0 until pExArr.length()) {
                db.plannedWorkoutDao().insertExercise(
                    PlannedExerciseEntity(workoutId = newId, exerciseName = pExArr.getString(j))
                )
            }
        }

        // --- свои упражнения ---
        val customArr = root.optJSONArray("customExercises") ?: JSONArray()
        for (i in 0 until customArr.length()) {
            val cObj = customArr.getJSONObject(i)
            db.customExerciseDao().insert(
                CustomExerciseEntity(
                    name = cObj.getString("name"),
                    muscleGroup = cObj.getString("muscleGroup")
                )
            )
        }
    }
}