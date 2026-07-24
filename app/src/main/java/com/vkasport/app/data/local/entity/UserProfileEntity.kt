package com.vkasport.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Профиль пользователя — одна строка (id всегда 1), по аналогии с
 * in_progress_workout. Добавлено в этапе «профиль» (миграция 16→17).
 *
 * photoPath — абсолютный путь к скопированному во внутреннюю память файлу
 * фото (в бэкап НЕ попадает: путь привязан к устройству).
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String? = null,
    val photoPath: String? = null,
    val heightCm: Float? = null,
    val weightKg: Float? = null
)