package com.vkasport.app.data.model

enum class MuscleGroup(
    val title: String
) {
    CHEST("Грудь"),
    BACK("Спина"),
    LEGS("Ноги"),
    SHOULDERS("Плечи"),
    BICEPS("Бицепс"),
    TRICEPS("Трицепс"),
    ABS("Пресс"),
    FOREARMS("Предплечья"),
    NECK("Шея"),
    STRETCH("Растяжка"),

    // ДОБАВЛЕНО (этап «новые группы»). Имена констант хранятся в БД —
    // не переименовывать. Новые группы добавлены в конец, чтобы
    // привычный порядок на экране выбора не поменялся.
    STREET("Улица"),
    HOME("Дом"),
    FAT_BURN("Жиросжигание")
}