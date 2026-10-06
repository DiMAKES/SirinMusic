package ru.rainedev.sirinmusic

import org.junit.Assert.*
import org.junit.Test
import ru.rainedev.sirinmusic.data.ConnectLink

class PendingLinkTest {
    private val link = ConnectLink("http://b.local:8787", "t-b")

    @Test fun unusedLinkIsStillThereForARecreatedScreen() {
        val pending = PendingLink() // lives in AppViewModel, which outlives the Activity
        pending.offer(link)
        // The first screen goes away before using the link; the recreated one sees it.
        assertEquals(link, pending.value.value)
        assertEquals(link, pending.value.value)
        assertEquals(link, pending.consume())
    }

    @Test fun consumedLinkIsHandedOutOnlyOnce() {
        val pending = PendingLink()
        pending.offer(link)
        assertEquals(link, pending.consume())
        // A screen recreated after sign-in started must not sign in again.
        assertNull(pending.value.value)
        assertNull(pending.consume())
    }

    @Test fun newerLinkReplacesAnUnusedOne() {
        val pending = PendingLink()
        pending.offer(ConnectLink("http://a.local:8787", "t-a"))
        pending.offer(link)
        assertEquals(link, pending.consume())
    }
}
