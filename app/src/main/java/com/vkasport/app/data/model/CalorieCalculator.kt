package com.vkasport.app.data.model

/**
 * Оценка сожжённых калорий за тренировку по MET
 * (metabolic equivalent of task): ккал = MET × вес(кг) × время(ч).
 *
 * Логика:
 *  - кардио-упражнения (CARDIO/DISTANCE и скакалка) считаются по своим
 *    MET и записанному в подходах времени
 *  - вся ОСТАЛЬНАЯ длительность тренировки считается силовой работой
 *    (MET 5.0 — силовая с усилием, включая отдых между подходами)
 *  - если вес атлета не введён — берём 75 кг
 *
 * Это оценка, а не измерение: реальный расход зависит от интенсивности,
 * пульса и обмена веществ.
 */
object CalorieCalculator {

    private const val STRENGTH_MET = 5.0f
    private const val DEFAULT_BODY_WEIGHT = 75f
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

        var cardioSeconds = 0
        var cardioKcal = 0f
        exercises.forEach { ex ->
            val isCardio = ex.measureType == MeasureType.CARDIO ||
                    ex.measureType == MeasureType.DISTANCE ||
                    CARDIO_MET.containsKey(ex.name)
            if (!isCardio) return@forEach
            val met = CARDIO_MET[ex.name] ?: DEFAULT_CARDIO_MET
            ex.sets.forEach { s ->
                val sec = s.seconds ?: 0
                cardioSeconds += sec
                cardioKcal += met * weight * (sec / 3600f)
            }
        }

        // Оставшееся время тренировки — силовая часть
        val strengthSeconds = (durationMinutes * 60 - cardioSeconds).coerceAtLeast(0)
        val strengthKcal = STRENGTH_MET * weight * (strengthSeconds / 3600f)

        return (strengthKcal + cardioKcal).toInt()
    }
}