package com.vkasport.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vkasport.app.MainActivity
import com.vkasport.app.R

/**
 * Уведомления-напоминания (отдельный канал от таймера отдыха):
 *  - утром в день запланированной тренировки (9:00)
 *  - за 2 часа до запланированной тренировки
 *  - если не было тренировок больше 2 дней
 *
 * Канал IMPORTANCE_DEFAULT — со звуком, в отличие от тихого таймера отдыха.
 */
object WorkoutReminders {

    private const val CHANNEL_ID = "workout_reminders"
    private const val INACTIVITY_NOTIFICATION_ID = 3000

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Напоминания о тренировках",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Запланированные тренировки и напоминания о перерывах"
                }
            )
        }
    }

    private fun show(context: Context, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_rest_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    // id уведомлений: чётные/нечётные от id запланированной тренировки,
    // чтобы «утреннее» и «за 2 часа» не перетирали друг друга
    fun showPlannedMorning(context: Context, muscleGroup: String, timeText: String, plannedId: Long) {
        show(
            context,
            (2000 + plannedId * 2).toInt(),
            "Сегодня тренировка 💪",
            "$muscleGroup в $timeText"
        )
    }

    fun showPlannedSoon(context: Context, muscleGroup: String, timeText: String, plannedId: Long) {
        show(
            context,
            (2001 + plannedId * 2).toInt(),
            "Тренировка через 2 часа",
            "$muscleGroup в $timeText"
        )
    }

    fun showInactivity(context: Context, days: Long) {
        show(
            context,
            INACTIVITY_NOTIFICATION_ID,
            "Пора в зал!",
            "Уже $days ${daysWord(days)} без тренировок"
        )
    }

    private fun daysWord(n: Long): String {
        val r10 = n % 10
        val r100 = n % 100
        return when {
            r100 in 11..14 -> "дней"
            r10 == 1L -> "день"
            r10 in 2..4 -> "дня"
            else -> "дней"
        }
    }
}