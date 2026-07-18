package com.vkasport.app.data.backup

import com.vkasport.app.data.local.database.WorkoutDatabase
import com.vkasport.app.data.local.entity.CompletedWorkoutEntity
import com.vkasport.app.data.local.entity.CompletedWorkoutExerciseEntity
import com.vkasport.app.data.local.entity.CompletedWorkoutSetEntity
import com.vkasport.app.data.local.entity.CustomExerciseEntity
import com.vkasport.app.data.local.entity.ExerciseHistoryEntity
import com.vkasport.app.data.local.entity.PlannedExerciseEntity
import com.vkasport.app.data.local.entity.PlannedWorkoutEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Экспорт/импорт всех данных приложения в JSON.
 *
 * ПРАВИЛА ВЕРСИОНИРОВАНИЯ ФОРМАТА (важно для «бэкапов разных версий»):
 *  - В каждом бэкапе есть поле "formatVersion". Сейчас = 1.
 *  - При изменении структуры данных (например, модель упражнений v2)
 *    formatVersion повышается, НО парсер старой версии (importV1)
 *    ОСТАЁТСЯ НАВСЕГДА — старые бэкапы обязаны восстанавливаться всегда.
 *  - Бэкап более НОВОЙ версии, чем знает приложение, не импортируем —
 *    честная ошибка «обновите приложение».
 *
 * Связи по id НЕ сохраняются в файле — упражнения и подходы вложены
 * внутрь своих тренировок, а при импорте получают свежие id из Room.
 */
object BackupManager {

    const val FORMAT_VERSION = 1

    // ==================== ЭКСПОРТ ====================

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
                exArr.put(exObj)
            }
            wObj.put("exercises", exArr)

            val setArr = JSONArray()
            db.completedWorkoutSetDao().getByWorkout(w.id).forEach { s ->
                val sObj = JSONObject()
                sObj.put("exerciseName", s.exerciseName)
                sObj.put("weight", s.weight.toDouble())
                sObj.put("reps", s.reps)
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
            customArr.put(cObj)
        }
        root.put("customExercises", customArr)

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
            else -> throw IllegalArgumentException(
                "Бэкап формата версии $v — эта версия приложения его не знает. Обновите приложение."
            )
        }
    }

    /** Парсер формата v1. НЕ РЕДАКТИРОВАТЬ при добавлении новых версий — только добавлять importV2 и т.д. */
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