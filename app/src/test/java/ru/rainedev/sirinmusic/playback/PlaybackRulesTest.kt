package ru.rainedev.sirinmusic.playback

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.rainedev.sirinmusic.data.EventReply
import ru.rainedev.sirinmusic.data.Track

class PlaybackRulesTest {
    @Test fun fixedPlaylistNeverFallsIntoRadio() {
        assertEquals(AdvanceAction.STOP, advanceAction(EventReply(ended = true, fixed = true), true))
        assertEquals(AdvanceAction.STOP, advanceAction(EventReply(), true))
    }
    @Test fun nextTrackWinsAndRadioMayContinue() {
        assertEquals(AdvanceAction.PLAY_NEXT, advanceAction(EventReply(next = Track(id = 42), fixed = true), true))
        assertEquals(AdvanceAction.START_RADIO, advanceAction(EventReply(), false))
        assertEquals(AdvanceAction.STOP, advanceAction(EventReply(ended = true), false))
    }
}
