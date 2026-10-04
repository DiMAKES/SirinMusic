package ru.rainedev.sirinmusic.playback

import ru.rainedev.sirinmusic.data.EventReply

enum class AdvanceAction { PLAY_NEXT, STOP, START_RADIO }

fun advanceAction(reply: EventReply, wasFixed: Boolean): AdvanceAction = when {
    reply.next != null -> AdvanceAction.PLAY_NEXT
    reply.ended || reply.fixed || wasFixed -> AdvanceAction.STOP
    else -> AdvanceAction.START_RADIO
}
