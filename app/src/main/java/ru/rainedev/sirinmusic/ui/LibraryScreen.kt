package ru.rainedev.sirinmusic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.rainedev.sirinmusic.AppState
import ru.rainedev.sirinmusic.playback.PlayerUi

@Composable
fun LibraryScreen(state: AppState, player: PlayerUi, artworkUrl: (String?, Int) -> String?,
    onTrack: (Long) -> Unit, onArtist: (String) -> Unit, onAlbum: (String, String) -> Unit,
    onFavorite: (Long) -> Unit, onArtistFavorite: (String) -> Unit, onAlbumFavorite: (String, String) -> Unit,
    onRate: (Long, String) -> Unit, onAddToPlaylist: (Long) -> Unit, contentPadding: PaddingValues,
    favoritesOnly: Boolean = false, selectionMode: Boolean = false) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var favoritesFilter by rememberSaveable { mutableStateOf(favoritesOnly) }
    val tracks = if (favoritesFilter) state.favorites else state.tracks
    val artists = if (favoritesFilter) state.favoriteArtistRows else state.artists
    val albums = if (favoritesFilter) state.favoriteAlbumRows else state.albums
    val favoriteIds = remember(state.favorites) { state.favorites.map { it.key }.toSet() }
    val needle = query.trim().lowercase()
    val shownTracks = remember(tracks, needle) { tracks.filter { needle.isEmpty() || listOfNotNull(it.title, it.artist, it.album).any { v -> v.lowercase().contains(needle) } } }
    val shownArtists = remember(artists, needle) { artists.filter { it.artist.lowercase().contains(needle) } }
    val shownAlbums = remember(albums, needle) { albums.filter { it.album.lowercase().contains(needle) || it.artist.lowercase().contains(needle) } }
    Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Поиск по библиотеке") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        if (!favoritesOnly) FilterChip(selected = favoritesFilter, onClick = { favoritesFilter = !favoritesFilter },
            label = { Text("Избранное") }, modifier = Modifier.padding(horizontal = 16.dp))
        if (selectionMode) Text("Нажми на трек, чтобы добавить его в плейлист", modifier = Modifier.padding(16.dp))
        PrimaryTabRow(selectedTabIndex = tab) {
            (if (selectionMode) listOf("Треки") else listOf("Треки", "Артисты", "Альбомы")).forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        if (state.libraryLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
            val empty = when(tab) { 0 -> shownTracks.isEmpty(); 1 -> shownArtists.isEmpty(); else -> shownAlbums.isEmpty() }
            if (empty && !state.libraryLoading) item { EmptyState(if (favoritesFilter) "В избранном пока пусто" else "Ничего не найдено", "Измени поиск или обнови библиотеку в профиле.") }
            when (tab) {
                0 -> items(shownTracks, key = { it.key }) { t ->
                    TrackRow(t, artworkUrl, t.key in favoriteIds, player.ratings[t.key], "favorite:${t.key}" in state.busy,
                        player.sessionId != null && t.key !in player.ratingPending,
                        { onTrack(t.key) }, { onFavorite(t.key) }, { onRate(t.key, it) }, { onAddToPlaylist(t.key) })
                }
                1 -> items(shownArtists, key = { it.artist }) { a ->
                    RowCard(artworkUrl(a.artwork ?: a.coverTrackId?.let { "/api/artwork/$it" }, 96), a.artist, "${a.tracks} треков",
                        trailing = { FavoriteButton(a.artist in state.favoriteArtists, "artist:${a.artist}" !in state.busy) { onArtistFavorite(a.artist) } },
                        onClick = { onArtist(a.artist) })
                }
                else -> items(shownAlbums, key = { "${it.artist.length}:${it.artist}${it.album}" }) { a ->
                    RowCard(artworkUrl(a.artwork ?: a.coverTrackId?.let { "/api/artwork/$it" }, 96), a.album, "${a.artist} · ${a.tracks} треков",
                        trailing = { FavoriteButton((a.artist to a.album) in state.favoriteAlbums, "album:${a.artist}/${a.album}" !in state.busy) { onAlbumFavorite(a.artist, a.album) } },
                        onClick = { onAlbum(a.artist, a.album) })
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp))
    }
}
