package com.vkasport.app.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Синхронизирует цвет статус-бара и цвет системных иконок (время/батарея)
 * с текущим экраном приложения. Вызывается из корневых composable-функций
 * каждого экрана/потока с соответствующими цветами.
 *
 * darkIcons = true  -> тёмные иконки (для светлого фона статус-бара)
 * darkIcons = false -> светлые иконки (для тёмного фона статус-бара)
 */
@Composable
fun SystemBarsAppearance(
    statusBarColor: Color,
    darkIcons: Boolean
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val activity = view.context as? Activity ?: return

    SideEffect {
        activity.window.statusBarColor = statusBarColor.toArgb()
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = darkIcons
    }
}