package ru.rainedev.sirinmusic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import ru.rainedev.sirinmusic.data.ConnectLink

/**
 * A musik://connect link opened from the system camera, waiting for the login screen.
 *
 * AppViewModel owns it, so a link that has not been used yet survives Activity
 * recreation (rotation, theme change). [consume] hands it out once and clears it, so a
 * screen recreated after that does not sign in again. Memory only: the link carries the
 * API token, which must not land in the saved-state Bundle as plain text.
 */
class PendingLink {
    private val link = MutableStateFlow<ConnectLink?>(null)
    val value: StateFlow<ConnectLink?> = link.asStateFlow()

    fun offer(next: ConnectLink) { link.value = next }

    fun consume(): ConnectLink? = link.getAndUpdate { null }
}
