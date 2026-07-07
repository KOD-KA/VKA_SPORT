package com.vkasport.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor
import com.vkasport.app.data.local.database.AppDatabaseProvider
import com.vkasport.app.data.repository.WorkoutRepository
import com.vkasport.app.ui.main.MainScreen
import com.vkasport.app.ui.theme.VKASPORTTheme
import com.vkasport.app.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {

    // Запрос разрешения на уведомления (обязательно с Android 13 / API 33).
    // Если пользователь откажет — приложение работает как раньше,
    // просто без таймера в шторке и без напоминаний.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ВАЖНО: начиная с targetSdk 35 Android принудительно включает
        // edge-to-edge для всех приложений — ручная установка
        // window.statusBarColor перестаёт действовать, и без явной
        // инициализации через enableEdgeToEdge() система может сама
        // решать, тёмные или светлые иконки статус-бара показывать,
        // ориентируясь на системную тему телефона (а не на наш экран).
        // Именно поэтому на чёрных экранах (старт, ввод веса, выбор
        // упражнений, тренировка, итоги) при светлой системной теме
        // телефона иконки были невидимыми. enableEdgeToEdge() правильно
        // инициализирует edge-to-edge с самого начала, после чего наши
        // рантайм-переключения иконок (SystemBarsAppearance) в Compose
        // снова работают предсказуемо на всех версиях Android.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val db = AppDatabaseProvider.getDatabase(this)

        val repository =
            WorkoutRepository(
                db.workoutDao()
            )

        val viewModel =
            WorkoutViewModel(
                repository
            )

        setContent {
            VKASPORTTheme {
                MainScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}