package ru.rainedev.sirinmusic.data

/** Пустой поиск возвращает исходный список без копии; сравнение не создаёт lowercase-строки. */
internal fun <T> filterLibrary(items: List<T>, query: String, matches: (T, String) -> Boolean): List<T> {
    val needle = query.trim()
    return if (needle.isEmpty()) items else items.filter { matches(it, needle) }
}

internal fun trackMatches(track: Track, needle: String): Boolean =
    track.title?.contains(needle, ignoreCase = true) == true ||
        track.artist?.contains(needle, ignoreCase = true) == true ||
        track.album?.contains(needle, ignoreCase = true) == true
