package ru.rainedev.sirinmusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.rainedev.sirinmusic.data.Mix
import ru.rainedev.sirinmusic.data.Track

@Composable
fun HomeScreen(
    mixes: List<Mix>,
    favorites: List<Track>,
    maturity: String?,
    artworkUrl: (String?, Int) -> String?,
    onRadio: () -> Unit,
    onMix: (String) -> Unit,
    onTrack: (Long) -> Unit,
    contentPadding: PaddingValues,
    onFavorites: () -> Unit,
) {
    LazyColumn(contentPadding = contentPadding) {
        item {
            Column(Modifier.padding(16.dp)) {
                Text("Sirin Music", style = MaterialTheme.typography.headlineMedium)
                if (maturity != null) {
                    Text(
                        "профиль вкуса: " + maturityRu(maturity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onRadio, modifier = Modifier.padding(top = 12.dp)) {
                    Icon(Icons.Rounded.Radio, contentDescription = null)
                    Text("Слушать радио", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        if (mixes.isNotEmpty()) {
            item { SectionTitle("Подборки") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(mixes, key = { it.kind }) { mix ->
                        MixCard(
                            mix = mix,
                            url = artworkUrl(mix.coverTrackId?.let { "/api/artwork/$it" }, 256),
                            onClick = { onMix(mix.kind) },
                        )
                    }
                }
            }
        }

        item { TextButton(onClick = onFavorites, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Всё избранное") } }
        if (favorites.isNotEmpty()) {
            item { SectionTitle("Избранное") }
            items(favorites.take(20), key = { it.key }) { track ->
                RowCard(
                    artworkUrl = artworkUrl(track.artworkPath, 96),
                    title = track.title ?: "—",
                    subtitle = track.artist,
                    onClick = { onTrack(track.key) },
                )
            }
        }
    }
}

@Composable
private fun MixCard(mix: Mix, url: String?, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.width(160.dp)) {
        Column {
            Artwork(url, 160.dp)
            Text(
                mix.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row {
                Text(
                    mix.subtitle ?: "${mix.tracks} треков",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

fun maturityRu(value: String): String = when (value) {
    "discovering" -> "знакомится"
    "forming" -> "складывается"
    "ready" -> "сложился"
    else -> value
}
