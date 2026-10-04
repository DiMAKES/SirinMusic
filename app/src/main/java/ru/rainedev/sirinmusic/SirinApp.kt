package ru.rainedev.sirinmusic

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import ru.rainedev.sirinmusic.data.MusikApi
import ru.rainedev.sirinmusic.data.Settings
import ru.rainedev.sirinmusic.playback.PlaybackController

class SirinApp : Application(), SingletonImageLoader.Factory {

    val settings: Settings by lazy { Settings(this) }
    val api: MusikApi by lazy { MusikApi(settings) }
    val playback: PlaybackController by lazy { PlaybackController(this, api, settings) }

    /**
     * Coil сам заголовки не подставит, а /api/artwork закрыт авторизацией —
     * поэтому отдаём ему наш OkHttp с тем же интерсептором, иначе все обложки 401.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { api.client }))
            }
            .build()
}
