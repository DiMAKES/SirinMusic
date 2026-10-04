package ru.rainedev.sirinmusic.data

import org.junit.Assert.*
import org.junit.Test

class LrcTest {
    @Test fun repeatedTimestampsFractionsAndMetadata() {
        val lines = parseLrc("[ar:Artist]\n[00:02.50][01:02.500]Chorus\n[00:01.2]First\n[01:70]Invalid")
        assertEquals(listOf(1.2, 2.5, 62.5), lines.map { it.seconds })
        assertEquals(listOf("First", "Chorus", "Chorus"), lines.map { it.text })
        assertEquals(-1, activeLyricIndex(lines, 0.0))
        assertEquals(1, activeLyricIndex(lines, 3.0))
    }
    @Test fun globalOffsetAndEmptyLines() {
        val lines = parseLrc("[offset:-500]\n[00:00.20]Intro\n[00:01.00]\n[00:02.00]Text")
        assertEquals(listOf(0.0, 0.5, 1.5), lines.map { it.seconds })
        assertEquals("", lines[1].text)
    }
    @Test fun plainTextAndMalformedInputHaveNoTimestamps() {
        assertTrue(parseLrc("Text only\n[bad]\n[999999999999999999999:01]overflow").isEmpty())
    }
}
