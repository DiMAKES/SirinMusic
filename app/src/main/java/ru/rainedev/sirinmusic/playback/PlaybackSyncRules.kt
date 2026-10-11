package ru.rainedev.sirinmusic.playback

import ru.rainedev.sirinmusic.data.PlaybackState
import ru.rainedev.sirinmusic.data.RadioStart
import ru.rainedev.sirinmusic.data.Track

fun safePlaybackPosition(position: Double, duration: Double): Double {
    val value = position.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
    return if (duration.isFinite() && duration > 0.0) value.coerceAtMost(duration) else value
}

/** A saved track is not enough: the live session supplies its impression and queue. */
fun restorableTrack(state: PlaybackState, now: RadioStart): Track? =
    now.current?.takeIf { now.sessionId == state.sessionId && it.key == state.trackId && it.ready }
