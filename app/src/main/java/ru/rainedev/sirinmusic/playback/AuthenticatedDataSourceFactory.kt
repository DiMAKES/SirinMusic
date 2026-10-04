package ru.rainedev.sirinmusic.playback

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import ru.rainedev.sirinmusic.data.MusikApi

/** Audio and notification artwork share live credentials and 401 handling with JSON/Coil. */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class AuthenticatedDataSourceFactory(api: MusikApi) : DataSource.Factory {
    private val factory = OkHttpDataSource.Factory(api.client)
    override fun createDataSource(): DataSource = factory.createDataSource()
}
