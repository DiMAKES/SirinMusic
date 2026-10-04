package ru.rainedev.sirinmusic.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import ru.rainedev.sirinmusic.data.Track

@Composable
fun FavoriteButton(selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(if (selected) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (selected) "Убрать из избранного" else "Добавить в избранное",
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TrackRow(track: Track, artworkUrl: (String?, Int) -> String?, favorite: Boolean,
    rating: String?, favoriteBusy: Boolean, ratingEnabled: Boolean,
    onPlay: () -> Unit, onFavorite: () -> Unit, onRate: (String) -> Unit, onAddToPlaylist: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    RowCard(artworkUrl(track.artworkPath, 96), track.title ?: "Без названия",
        listOfNotNull(track.artist, track.album, if (!track.ready) "Пока недоступен" else null).joinToString(" · "),
        trailing = {
            Row {
                FavoriteButton(favorite, !favoriteBusy, onFavorite)
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Действия с треком") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(if (rating == "like") "Нравится ✓" else "Нравится") },
                        leadingIcon = { Icon(Icons.Rounded.ThumbUp, null) }, enabled = ratingEnabled && rating != "like",
                        onClick = { menu = false; onRate("like") })
                    DropdownMenuItem(text = { Text(if (rating == "dislike") "Не нравится ✓" else "Не нравится") },
                        leadingIcon = { Icon(Icons.Rounded.ThumbDown, null) }, enabled = ratingEnabled && rating != "dislike",
                        onClick = { menu = false; onRate("dislike") })
                    DropdownMenuItem(text = { Text("В плейлист") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                        onClick = { menu = false; onAddToPlaylist() })
                }
            }
        }, onClick = { if (track.ready) onPlay() })
}
