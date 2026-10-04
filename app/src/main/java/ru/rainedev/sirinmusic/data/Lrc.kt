package ru.rainedev.sirinmusic.data

/** Multiple timestamps, fractional seconds and LRC's global millisecond offset. */
data class LyricLine(val seconds: Double, val text: String)

fun parseLrc(text: String): List<LyricLine> {
    val stamp = Regex("\\[(\\d+):(\\d{2})(?:[.:](\\d{1,3}))?]")
    val offset = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE)
        .find(text)?.groupValues?.get(1)?.toLongOrNull()?.div(1000.0) ?: 0.0
    return text.lineSequence().flatMap { line ->
        val matches = stamp.findAll(line).toList()
        val words = stamp.replace(line, "").trim()
        matches.asSequence().mapNotNull { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val seconds = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            if (seconds >= 60) return@mapNotNull null
            val fraction = match.groupValues[3].let { if (it.isEmpty()) 0.0 else "0.$it".toDouble() }
            LyricLine((minutes * 60.0 + seconds + fraction + offset).coerceAtLeast(0.0), words)
        }
    }.sortedBy { it.seconds }.toList()
}

fun activeLyricIndex(lines: List<LyricLine>, position: Double): Int = lines.indexOfLast { it.seconds <= position }
