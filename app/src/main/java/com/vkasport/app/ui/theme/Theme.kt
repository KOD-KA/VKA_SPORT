package com.vkasport.app.ui.theme


import androidx.compose.foundation.isSystemInDarkTheme
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

    darkTheme: Boolean = isSystemInDarkTheme(),

    content: @Composable () -> Unit

) {


    val colors =

        if (darkTheme) {

            DarkColors

        } else {

            LightColors

        }



    MaterialTheme(

        colorScheme = colors,

        typography = Typography(),

        content = content

    )

}