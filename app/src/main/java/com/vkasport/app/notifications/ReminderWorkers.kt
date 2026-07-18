package com.vkasport.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vkasport.app.data.local.database.AppDatabaseProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Фоновые задачи WorkManager. WorkManager сам переживает перезагрузку
 * телефона и закрытие приложения — отдельный BroadcastReceiver на
 * BOOT_COMPLETED не нужен.
 */

/**
 * Разовое напоминание о запланированной тренировке.
 * inputData: "type" ("morning" | "soon"), "plannedId", "muscleGroup", "timeText".
 */
class PlannedWorkoutReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val plannedId = inputData.getLong("plannedId", -1L)
        val type = inputData.getString("type") ?: return Result.success()
        val muscleGroup = inputData.getString("muscleGroup") ?: ""
        val timeText = inputData.getString("timeText") ?: ""

        // Страховка: если тренировку уже удалили из плана —
        // молча выходим (даже если cancel по какой-то причине не сработал)
        val db = AppDatabaseProvider.getDatabase(applicationContext)
        val stillPlanned = db.plannedWorkoutDao().getAll().any { it.id == plannedId }
        if (!stillPlanned) return Result.success()

        when (type) {
            "morning" -> WorkoutReminders.showPlannedMorning(
                applicationContext, muscleGroup, timeText, plannedId
            )
            "soon" -> WorkoutReminders.showPlannedSoon(
                applicationContext, muscleGroup, timeText, plannedId
            )
        }
        return Result.success()
    }
}

/**
 * Ежедневная проверка: если с последней завершённой тренировки прошло
 * БОЛЬШЕ 2 дней — напоминаем. Если тренировок ещё не было вообще —
 * не навязываемся.
 */
class InactivityCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabaseProvider.getDatabase(applicationContext)

        // getAllWorkouts() отсортирован по date DESC — первый и есть последний
        val latest = db.workoutHistoryDao().getAllWorkouts().firstOrNull()
            ?: return Result.success()

        val lastDate = Instant.ofEpochMilli(latest.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val days = ChronoUnit.DAYS.between(lastDate, LocalDate.now())

        if (days > 2) {
            WorkoutReminders.showInactivity(applicationContext, days)
        }
        return Result.success()
    }
}