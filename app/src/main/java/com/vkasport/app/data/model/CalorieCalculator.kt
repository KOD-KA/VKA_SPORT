package com.vkasport.app.data.model

import kotlin.math.roundToInt

/**
 * Оценка сожжённых калорий за тренировку по MET
 * (metabolic equivalent of task): ккал = MET × вес(кг) × время(ч).
 *
 * БАГ 7 (переписано): раньше вся силовая часть считалась от ОБЩЕЙ
 * длительности тренировки. Если пользователь вводил тренировку быстро
 * (длительность 10 сек), 100 отжиманий давали ~8 ккал — бессмыслица.
 *
 * Теперь активное время считается ИЗ САМИХ ПОДХОДОВ и не зависит от
 * длительности на секундомере:
 *  - силовые / повторы: ~3 секунды активной работы на повтор
 *  - статика (время): по записанным секундам удержания
 *  - кардио / дистанция: по записанному времени (а если время не
 *    введено, оценивается из дистанции при скорости ~9 км/ч)
 *
 * Это оценка, а не измерение: реальный расход зависит от интенсивности,
 * пульса и обмена веществ.
 */
object CalorieCalculator {

    private const val DEFAULT_BODY_WEIGHT = 75f
    private const val SECONDS_PER_REP = 3f    // активное время на один повтор
    private const val STRENGTH_MET = 5.0f     // силовая работа с усилием
    private const val ISOMETRIC_MET = 4.0f    // статика: планка, вис, удержание
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

    /**
     * durationMinutes больше НЕ используется для расчёта (оставлен в
     * сигнатуре для совместимости вызовов) — время берётся из подходов.
     */
    fun estimateKcal(
        exercises: List<WorkoutExercise>,
        durationMinutes: Long,
        athleteWeight: Float?
    ): Int {
        val weight = athleteWeight ?: DEFAULT_BODY_WEIGHT
        var kcal = 0f

        exercises.forEach { ex ->
            val isCardioByName = CARDIO_MET.containsKey(ex.name)
            when {
                // ── Кардио и дистанция ────────────────────────────────
                ex.measureType == MeasureType.CARDIO ||
                        ex.measureType == MeasureType.DISTANCE ||
                        isCardioByName -> {
                    val met = CARDIO_MET[ex.name] ?: DEFAULT_CARDIO_MET
                    ex.sets.forEach { s ->
                        var sec = s.seconds ?: 0
                        // время не введено, но есть дистанция → оценка при ~9 км/ч
                        if (sec == 0 && (s.distanceKm ?: 0f) > 0f) {
                            sec = ((s.distanceKm!! / 9f) * 3600f).toInt()
                        }
                        kcal += met * weight * (sec / 3600f)
                    }
                }
                // ── Статика (удержание по времени) ────────────────────
                ex.measureType == MeasureType.TIME -> {
                    ex.sets.forEach { s ->
                        kcal += ISOMETRIC_MET * weight * ((s.seconds ?: 0) / 3600f)
                    }
                }
                // ── Силовые и «только повторы» ────────────────────────
                else -> {
                    ex.sets.forEach { s ->
                        val activeSec = s.reps * SECONDS_PER_REP
                        kcal += STRENGTH_MET * weight * (activeSec / 3600f)
                    }
                }
            }
        }

        return kcal.roundToInt()
    }
}