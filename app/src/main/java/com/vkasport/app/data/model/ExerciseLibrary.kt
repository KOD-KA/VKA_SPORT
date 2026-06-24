package com.vkasport.app.data.model

object ExerciseLibrary {

    val exercises = listOf(

        // ===== ГРУДЬ =====
        ExerciseCatalog("Жим штанги лёжа",          MuscleGroup.CHEST),
        ExerciseCatalog("Жим гантелей лёжа",         MuscleGroup.CHEST),
        ExerciseCatalog("Разводка гантелей лёжа",    MuscleGroup.CHEST),
        ExerciseCatalog("Жим в тренажёре сидя",      MuscleGroup.CHEST),
        ExerciseCatalog("Отжимания",                  MuscleGroup.CHEST),

        // ===== СПИНА =====
        ExerciseCatalog("Становая тяга",              MuscleGroup.BACK),
        ExerciseCatalog("Тяга верхнего блока",        MuscleGroup.BACK),
        ExerciseCatalog("Подтягивания",               MuscleGroup.BACK),
        ExerciseCatalog("Тяга штанги в наклоне",      MuscleGroup.BACK),
        ExerciseCatalog("Тяга гантели одной рукой",   MuscleGroup.BACK),

        // ===== НОГИ =====
        ExerciseCatalog("Приседания со штангой",      MuscleGroup.LEGS),
        ExerciseCatalog("Жим ногами",                 MuscleGroup.LEGS),
        ExerciseCatalog("Румынская тяга",             MuscleGroup.LEGS),
        ExerciseCatalog("Выпады с гантелями",         MuscleGroup.LEGS),
        ExerciseCatalog("Сгибание ног в тренажёре",   MuscleGroup.LEGS),

        // ===== ПЛЕЧИ =====
        ExerciseCatalog("Жим штанги стоя",            MuscleGroup.SHOULDERS),
        ExerciseCatalog("Жим гантелей сидя",          MuscleGroup.SHOULDERS),
        ExerciseCatalog("Разводка гантелей в стороны",MuscleGroup.SHOULDERS),
        ExerciseCatalog("Тяга штанги к подбородку",   MuscleGroup.SHOULDERS),

        // ===== БИЦЕПС =====
        ExerciseCatalog("Подъем штанги на бицепс",   MuscleGroup.BICEPS),
        ExerciseCatalog("Подъем гантелей на бицепс", MuscleGroup.BICEPS),
        ExerciseCatalog("Молотки с гантелями",        MuscleGroup.BICEPS),

        // ===== ТРИЦЕПС =====
        ExerciseCatalog("Французский жим",            MuscleGroup.TRICEPS),
        ExerciseCatalog("Разгибание рук на блоке",    MuscleGroup.TRICEPS),
        ExerciseCatalog("Жим узким хватом",           MuscleGroup.TRICEPS)
    )
}