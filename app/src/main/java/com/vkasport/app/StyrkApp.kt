package com.vkasport.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder

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
                // minSdk = 29, поэтому системный декодер анимации доступен
                // всегда (GifDecoder для API < 28 не нужен)
                add(AnimatedImageDecoder.Factory())
            }
            .build()
    }
}