package ru.rainedev.sirinmusic.playback

import org.junit.Assert.*
import org.junit.Test
import ru.rainedev.sirinmusic.data.*

class PlaybackSyncRulesTest {
    @Test fun restorationRequiresTheSameLiveSessionAndTrack() {
        val saved = PlaybackState("live",42,35.0,20.0,true,"browser")
        val now = RadioStart(sessionId="live",current=Track(id=42,impressionId="original"))
        assertEquals("original",restorableTrack(saved,now)?.impressionId)
        assertNull(restorableTrack(saved,now.copy(sessionId="other")))
        assertNull(restorableTrack(saved,now.copy(current=Track(id=43))))
        assertNull(restorableTrack(saved,now.copy(current=Track(id=42,ready=false))))
        assertNull(restorableTrack(saved,now.copy(current=null)))
    }
    @Test fun savedPositionsCannotSeekOutsideTheTrackOrPropagateNonFiniteValues() {
        assertEquals(0.0,safePlaybackPosition(Double.NaN,60.0),0.0)
        assertEquals(0.0,safePlaybackPosition(-5.0,60.0),0.0)
        assertEquals(60.0,safePlaybackPosition(100.0,60.0),0.0)
        assertEquals(35.0,safePlaybackPosition(35.0,0.0),0.0)
    }
}
