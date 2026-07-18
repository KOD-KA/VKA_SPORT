package com.vkasport.app.data.model

object ExerciseLibrary {

    val exercises = listOf(

        // ===== ГРУДЬ =====
        ExerciseCatalog("Жим штанги лёжа",              MuscleGroup.CHEST),
        ExerciseCatalog("Жим штанги на наклонной скамье", MuscleGroup.CHEST),
        ExerciseCatalog("Жим гантелей лёжа",             MuscleGroup.CHEST),
        ExerciseCatalog("Жим гантелей на наклонной скамье", MuscleGroup.CHEST),
        ExerciseCatalog("Разводка гантелей лёжа",        MuscleGroup.CHEST),
        ExerciseCatalog("Разводка в кроссовере",          MuscleGroup.CHEST),
        ExerciseCatalog("Жим в тренажёре сидя",          MuscleGroup.CHEST),
        ExerciseCatalog("Пуловер с гантелей",             MuscleGroup.CHEST),
        ExerciseCatalog("Отжимания",                       MuscleGroup.CHEST),
        ExerciseCatalog("Отжимания на брусьях",           MuscleGroup.CHEST),

        // ===== СПИНА =====
        ExerciseCatalog("Становая тяга",                  MuscleGroup.BACK),
        ExerciseCatalog("Тяга верхнего блока",            MuscleGroup.BACK),
        ExerciseCatalog("Тяга верхнего блока узким хватом", MuscleGroup.BACK),
        ExerciseCatalog("Подтягивания",                   MuscleGroup.BACK),
        ExerciseCatalog("Тяга штанги в наклоне",          MuscleGroup.BACK),
        ExerciseCatalog("Тяга гантели одной рукой",       MuscleGroup.BACK),
        ExerciseCatalog("Горизонтальная тяга блока",      MuscleGroup.BACK),
        ExerciseCatalog("Гиперэкстензия",                  MuscleGroup.BACK),
        ExerciseCatalog("Шраги со штангой",                MuscleGroup.BACK),
        ExerciseCatalog("Тяга Т-грифа",                    MuscleGroup.BACK),

        // ===== НОГИ =====
        ExerciseCatalog("Приседания со штангой",          MuscleGroup.LEGS),
        ExerciseCatalog("Фронтальные приседания",         MuscleGroup.LEGS),
        ExerciseCatalog("Жим ногами",                     MuscleGroup.LEGS),
        ExerciseCatalog("Румынская тяга",                 MuscleGroup.LEGS),
        ExerciseCatalog("Выпады с гантелями",             MuscleGroup.LEGS),
        ExerciseCatalog("Болгарские выпады",              MuscleGroup.LEGS),
        ExerciseCatalog("Сгибание ног в тренажёре",       MuscleGroup.LEGS),
        ExerciseCatalog("Разгибание ног в тренажёре",     MuscleGroup.LEGS),
        ExerciseCatalog("Подъемы на носки стоя",          MuscleGroup.LEGS),
        ExerciseCatalog("Гакк-приседания",                 MuscleGroup.LEGS),

        // ===== ПЛЕЧИ =====
        ExerciseCatalog("Жим штанги стоя",                MuscleGroup.SHOULDERS),
        ExerciseCatalog("Жим гантелей сидя",              MuscleGroup.SHOULDERS),
        ExerciseCatalog("Жим Арнольда",                    MuscleGroup.SHOULDERS),
        ExerciseCatalog("Разводка гантелей в стороны",    MuscleGroup.SHOULDERS),
        ExerciseCatalog("Тяга штанги к подбородку",       MuscleGroup.SHOULDERS),
        ExerciseCatalog("Махи гантелями в наклоне",       MuscleGroup.SHOULDERS),
        ExerciseCatalog("Разводка в кроссовере на плечи", MuscleGroup.SHOULDERS),

        // ===== БИЦЕПС =====
        ExerciseCatalog("Подъем штанги на бицепс",        MuscleGroup.BICEPS),
        ExerciseCatalog("Подъем гантелей на бицепс",      MuscleGroup.BICEPS),
        ExerciseCatalog("Молотки с гантелями",             MuscleGroup.BICEPS),
        ExerciseCatalog("Подъем на скамье Скотта",         MuscleGroup.BICEPS),
        ExerciseCatalog("Концентрированный подъем",       MuscleGroup.BICEPS),
        ExerciseCatalog("Подъем штанги обратным хватом",  MuscleGroup.BICEPS),

        // ===== ТРИЦЕПС =====
        ExerciseCatalog("Французский жим",                MuscleGroup.TRICEPS),
        ExerciseCatalog("Разгибание рук на блоке",        MuscleGroup.TRICEPS),
        ExerciseCatalog("Жим узким хватом",               MuscleGroup.TRICEPS),
        ExerciseCatalog("Разгибание руки с гантелью из-за головы", MuscleGroup.TRICEPS),
        ExerciseCatalog("Разгибание на блоке одной рукой", MuscleGroup.TRICEPS),

        // ===== ПРЕСС =====
        ExerciseCatalog("Скручивания",                     MuscleGroup.ABS),
        ExerciseCatalog("Подъем ног в висе",               MuscleGroup.ABS),
        // ИЗМЕНЕНО (модель v2): планка считается ВРЕМЕНЕМ удержания
        ExerciseCatalog("Планка",                           MuscleGroup.ABS, MeasureType.TIME),
        ExerciseCatalog("Скручивания на блоке",            MuscleGroup.ABS),
        ExerciseCatalog("Русские скручивания",             MuscleGroup.ABS),
        ExerciseCatalog("Велосипед",                        MuscleGroup.ABS),
        ExerciseCatalog("Подъем корпуса на наклонной скамье", MuscleGroup.ABS),

        // ===== ПРЕДПЛЕЧЬЯ =====
        ExerciseCatalog("Сгибание запястий со штангой",   MuscleGroup.FOREARMS),
        ExerciseCatalog("Разгибание запястий со штангой", MuscleGroup.FOREARMS),
        ExerciseCatalog("Сгибание запястий с гантелями",  MuscleGroup.FOREARMS),
        // ИЗМЕНЕНО (модель v2): удержание — время
        ExerciseCatalog("Удержание штанги на время",       MuscleGroup.FOREARMS, MeasureType.TIME),
        ExerciseCatalog("Кистевой эспандер",               MuscleGroup.FOREARMS, MeasureType.REPS),

        // ===== ШЕЯ =====
        ExerciseCatalog("Наклоны головы с сопротивлением", MuscleGroup.NECK),
        ExerciseCatalog("Круговые движения шеей",          MuscleGroup.NECK, MeasureType.REPS),
        ExerciseCatalog("Подъем головы лёжа",              MuscleGroup.NECK),

        // ===== РАСТЯЖКА =====
        // ИЗМЕНЕНО (модель v2): вся растяжка — ВРЕМЯ удержания, а не вес×повторы
        // (закрывает старую известную проблему из handoff-документа)
        ExerciseCatalog("Растяжка грудных мышц",           MuscleGroup.STRETCH, MeasureType.TIME),
        ExerciseCatalog("Растяжка спины (кошка-корова)",   MuscleGroup.STRETCH, MeasureType.TIME),
        ExerciseCatalog("Растяжка квадрицепса",            MuscleGroup.STRETCH, MeasureType.TIME),
        ExerciseCatalog("Растяжка задней поверхности бедра", MuscleGroup.STRETCH, MeasureType.TIME),
        ExerciseCatalog("Растяжка плеч",                    MuscleGroup.STRETCH, MeasureType.TIME),
        ExerciseCatalog("Растяжка трицепса",                MuscleGroup.STRETCH, MeasureType.TIME),
        ExerciseCatalog("Растяжка икроножных мышц",         MuscleGroup.STRETCH, MeasureType.TIME),
        ExerciseCatalog("Растяжка бицепса",                 MuscleGroup.STRETCH, MeasureType.TIME)
    )
}