package ru.rainedev.sirinmusic.data

import android.content.Context
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Удаляет только кеш изображений, не настройки и не буфер воспроизведения. */
class ImageCache(private val context: Context) {
    private val mutex = Mutex()
    suspend fun diskBytes(): Long = withContext(Dispatchers.IO) {
        SingletonImageLoader.get(context).diskCache?.size ?: 0L
    }
    suspend fun clear() = mutex.withLock {
        val loader = SingletonImageLoader.get(context)
        loader.memoryCache?.clear()
        withContext(Dispatchers.IO) { loader.diskCache?.clear() }
    }
}
