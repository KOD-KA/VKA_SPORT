package com.vkasport.app.data.model

import kotlin.math.roundToInt

/**
 * Оценка сожжённых калорий за тренировку по MET
 * (metabolic equivalent of task): ккал = MET × вес(кг) × время(ч).
 *
 * ПЕРЕРАБОТАНО (п3, v1.5.1). Прошлая версия считала ТОЛЬКО активное время
 * подходов и игнорировала общую длительность — из-за этого длинная
 * тренировка (2ч40, 11 тонн) давала лишь ~356 ккал: время отдыха между
 * подходами вообще не учитывалось.
 *
 * Новая модель разделяет тренировку на три части:
 *  1. Кардио (дорожка/вело/скакалка…) — по записанному времени и своему MET.
 *  2. Активная силовая работа — оценка из подходов/повторов
 *     (≈3 сек/повтор, но не меньше ≈30 сек на подход) при «энергичном» MET.
 *     Статика (планка/вис) — по записанным секундам удержания.
 *  3. Отдых/остальное время тренировки (общая длительность минус активное
 *     время) — при низком MET (стоя/лёгкая активность между подходами).
 *
 * Так короткая быстрозаписанная тренировка получает адекватный «пол» по
 * подходам, а длинная — учитывает всё время в зале.
 *
 * Это оценка, а не измерение: реальный расход зависит от интенсивности,
 * пульса и обмена веществ.
 */
object CalorieCalculator {

    private const val DEFAULT_BODY_WEIGHT = 75f
    private const val SECONDS_PER_REP = 3f       // активная работа на повтор
    private const val MIN_SECONDS_PER_SET = 30f  // подход длится не меньше ~30 сек
    private const val STRENGTH_ACTIVE_MET = 6.0f // энергичная силовая работа
    private const val ISOMETRIC_MET = 4.0f       // статика: планка, вис, удержание
    private const val REST_MET = 1.8f            // отдых/стоя между подходами
    private const val DEFAULT_CARDIO_MET = 7f

    // MET по названию кардио-упражнения (умеренная интенсивность)
    private val CARDIO_MET = mapOf(
        "Беговая дорожка"  to 9.0f,
        "Бег на улице"     to 9.8f,
        "Велотренажёр"     to 7.0f,
        "Велопрогулка"     to 6.8f,
        "Эллипсоид"        to 5.5f,
        "Гребной тренажёр" to 7.0f,
        "Степпер"          to 8.5f,
        "Ходьба в горку"   to 6.0f,
        "Скакалка"         to 11.0f,
        "Лыжи"             to 9.0f,
        "Плавание"         to 7.0f
    )

    fun estimateKcal(
        exercises: List<WorkoutExercise>,
        durationMinutes: Long,
        athleteWeight: Float?
    ): Int {
        val weight = athleteWeight ?: DEFAULT_BODY_WEIGHT

        var cardioKcal = 0f
        var cardioSeconds = 0f
        var timeKcal = 0f
        var timeSeconds = 0f
        var strengthReps = 0
        var strengthSets = 0

        exercises.forEach { ex ->
            val isCardioByName = CARDIO_MET.containsKey(ex.name)
            when {
                // ── Кардио и дистанция ────────────────────────────────
                ex.measureType == MeasureType.CARDIO ||
                        ex.measureType == MeasureType.DISTANCE ||
                        isCardioByName -> {
                    val met = CARDIO_MET[ex.name] ?: DEFAULT_CARDIO_MET
                    ex.sets.forEach { s ->
                        var sec = (s.seconds ?: 0).toFloat()
                        // время не введено, но есть дистанция → оценка при ~9 км/ч
                        if (sec == 0f && (s.distanceKm ?: 0f) > 0f) {
                            sec = (s.distanceKm!! / 9f) * 3600f
                        }
                        cardioSeconds += sec
                        cardioKcal += met * weight * (sec / 3600f)
                    }
                }
                // ── Статика (удержание по времени) ────────────────────
                ex.measureType == MeasureType.TIME -> {
                    ex.sets.forEach { s ->
                        val sec = (s.seconds ?: 0).toFloat()
                        timeSeconds += sec
                        timeKcal += ISOMETRIC_MET * weight * (sec / 3600f)
                    }
                }
                // ── Силовые и «только повторы» ────────────────────────
                else -> {
                    ex.sets.forEach { s ->
                        strengthReps += s.reps
                        strengthSets += 1
                    }
                }
            }
        }

        // Активное силовое время: по повторам, но не меньше ~30 сек на подход
        val strengthActiveSeconds = maxOf(strengthReps * SECONDS_PER_REP, strengthSets * MIN_SECONDS_PER_SET)
        val strengthKcal = STRENGTH_ACTIVE_MET * weight * (strengthActiveSeconds / 3600f)

        // Отдых = общая длительность минус всё активное время (кардио+статика+силовое)
        val totalSeconds = durationMinutes * 60f
        val activeSeconds = cardioSeconds + timeSeconds + strengthActiveSeconds
        val restSeconds = (totalSeconds - activeSeconds).coerceAtLeast(0f)
        val restKcal = REST_MET * weight * (restSeconds / 3600f)

        return (cardioKcal + timeKcal + strengthKcal + restKcal).roundToInt()
    }
}