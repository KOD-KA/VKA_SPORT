package com.vkasport.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
    lightColorScheme(
        primary = PrimaryLight,
        background = BackgroundLight,
        surface = SurfaceLight,
        onPrimary = White,
        onBackground = TextPrimaryLight,
        onSurface = TextPrimaryLight,
        secondary = SoftGray
    )

// Тёмная схема оставлена в коде на будущее, но сейчас НЕ используется:
// весь UI приложения (карточки, шапки, текст) спроектирован с жёстко
// заданными цветами (Black/White/SoftGray) в расчёте на светлый фон.
// Если разрешить авто-переключение по системной теме телефона, текст без
// явного цвета наследует onSurface/onBackground = White из тёмной схемы
// и становится невидимым на белых/светло-серых карточках — это и была
// причина "пропавших" надписей.
private val DarkColors =
    darkColorScheme(
        primary = PrimaryDark,
        background = BackgroundDark,
        surface = SurfaceDark,
        onPrimary = Black,
        onBackground = TextPrimaryDark,
        onSurface = TextPrimaryDark,
        secondary = DarkGray
    )

@Composable
fun VKASPORTTheme(
    // ВАЖНО: по умолчанию false, а не isSystemInDarkTheme().
    // Приложение всегда использует светлую схему независимо от системной
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}