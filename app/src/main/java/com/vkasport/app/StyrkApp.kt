package com.vkasport.app

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder

/**
 * Класс приложения. Нужен, чтобы включить декодер анимированных GIF в Coil
 * (по умолчанию Coil3 не анимирует гифки — надо явно добавить декодер).
 *
 * Подключается в манифесте через android:name=".StyrkApp".
 */
class StyrkApp : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}