package com.vkasport.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Одна запись замеров тела (этап «тело», миграция 15->16).
 * Все значения опциональны — можно записать только вес, только талию и т.д.
 * Размеры в сантиметрах, вес в килограммах.
 */
@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,              // LocalDate.toEpochDay()
    val weight: Float? = null,   // вес, кг
    val chest: Float? = null,    // грудь, см
    val waist: Float? = null,    // талия, см
    val hips: Float? = null,     // бёдра, см
    val biceps: Float? = null,   // бицепс, см
    val forearm: Float? = null,  // предплечье, см
    val thigh: Float? = null,    // бедро, см
    val calf: Float? = null,     // икра, см
    val neck: Float? = null,     // шея, см
    val shoulders: Float? = null // плечи, см
)