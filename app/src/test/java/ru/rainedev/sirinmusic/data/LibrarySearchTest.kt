package ru.rainedev.sirinmusic.data

import org.junit.Assert.*
import org.junit.Test

class LibrarySearchTest {
    @Test fun emptyQueryDoesNotDuplicateTheLibrary() {
        val tracks = List(23000) { Track(id = it.toLong(), title = "Song $it") }
        assertSame(tracks, filterLibrary(tracks, "   ", ::trackMatches))
    }
    @Test fun searchMatchesEveryFieldAndHandlesMissingMetadata() {
        val tracks = listOf(Track(id = 1, title = "ПОЛЁТ"), Track(id = 2, artist = "Полёт"),
            Track(id = 3, album = "Полёт"), Track(id = 4), Track(id = 5, title = "Other"))
        assertEquals(listOf(1L, 2L, 3L), filterLibrary(tracks, " полёт ", ::trackMatches).map { it.key })
        assertTrue(filterLibrary(tracks, "absent", ::trackMatches).isEmpty())
    }
}
