package ru.rainedev.sirinmusic.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.rainedev.sirinmusic.playback.PlayerUi

@Composable
fun NowPlayingScreen(
    ui: PlayerUi,
    artworkUrl: (String?, Int) -> String?,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onDislike: () -> Unit,
    onFavorite: () -> Unit,
    onSeek: (Double) -> Unit,
    onJump: (Long, Int) -> Unit,
    contentPadding: PaddingValues,
    onLike: () -> Unit,
    onLyrics: () -> Unit,
    onAddToPlaylist: () -> Unit,
    favoriteBusy: Boolean,
    onPrevious: () -> Unit,
) {
    val track = ui.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BoxWithConstraints { Artwork(artworkUrl(track?.artworkPath, 640), maxWidth.coerceAtMost(320.dp), corner = 20.dp) }

                Text(
                    track?.title ?: "Ничего не играет",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    listOfNotNull(track?.artist, track?.album).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (track?.explanation != null) {
                    Text(
                        "почему: ${track.explanation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Slider(
                    value = ui.positionSec.toFloat().coerceIn(0f, (ui.durationSec.toFloat()).coerceAtLeast(1f)),
                    enabled = track != null && ui.durationSec > 0,
                    onValueChange = { onSeek(it.toDouble()) },
                    valueRange = 0f..(if (ui.durationSec > 0) ui.durationSec.toFloat() else 1f),
                    modifier = Modifier.padding(top = 16.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(ui.positionSec), style = MaterialTheme.typography.bodySmall)
                    Text(formatTime(ui.durationSec), style = MaterialTheme.typography.bodySmall)
                }

                if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = onPrevious, enabled = track != null && !ui.busy) { Icon(Icons.Rounded.SkipPrevious, "Предыдущий трек") }
                    FilledIconButton(onClick = onToggle, enabled = track != null && !ui.busy, modifier = Modifier.size(72.dp)) {
                        Icon(if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (ui.isPlaying) "Пауза" else "Играть")
                    }
                    IconButton(onClick = onSkip, enabled = track != null && !ui.busy) { Icon(Icons.Rounded.SkipNext, "Дальше") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val pending = track?.key in ui.ratingPending
                    IconButton(onClick = onLike, enabled = track != null && !pending && ui.ratings[track.key] != "like") {
                        Icon(Icons.Rounded.ThumbUp, "Нравится", tint = if (ui.ratings[track?.key] == "like") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDislike, enabled = track != null && !pending && ui.ratings[track.key] != "dislike") {
                        Icon(Icons.Rounded.ThumbDown, "Не нравится", tint = if (ui.ratings[track?.key] == "dislike") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FavoriteButton(track != null && track.key in ui.favorites, track != null && !favoriteBusy, onFavorite)
                }
                Row {
                    TextButton(onClick = onLyrics, enabled = track != null) { Text("Текст песни") }
                    TextButton(onClick = onAddToPlaylist, enabled = track != null) { Text("В плейлист") }
                }

                if (ui.error != null) {
                    Text(
                        ui.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }

        if (ui.queue.isNotEmpty()) {
            item { SectionTitle(if (ui.fixed) "Треки плейлиста" else "Дальше в очереди") }
            itemsIndexed(ui.queue, key = { index, t -> "$index:${t.key}" }) { index, t ->
                RowCard(
                    artworkUrl = artworkUrl(t.artworkPath, 96),
                    title = t.title ?: "—",
                    subtitle = listOfNotNull(t.artist, t.explanation).joinToString(" · "),
                    onClick = { onJump(t.key, t.position ?: index) },
                )
            }
        }
    }
}
