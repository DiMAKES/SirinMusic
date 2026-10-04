package ru.rainedev.sirinmusic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.rainedev.sirinmusic.AppState
import ru.rainedev.sirinmusic.data.Playlist

@Composable
fun PlaylistsScreen(state: AppState, artworkUrl: (String?, Int) -> String?, onOpen: (Long) -> Unit,
    onCreate: (String, String) -> Unit, onRefresh: () -> Unit, contentPadding: PaddingValues) {
    var creating by rememberSaveable { mutableStateOf(false) }
    LazyColumn(contentPadding = contentPadding) {
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { creating = true }, enabled = "playlist-mutation" !in state.busy) {
                    Icon(Icons.Rounded.Add, null); Text("Создать", Modifier.padding(start = 8.dp))
                }
                IconButton(onClick = onRefresh, enabled = !state.playlistsLoading) { Icon(Icons.Rounded.Refresh, "Обновить плейлисты") }
            }
        }
        if (state.playlistsLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (state.playlists.isEmpty() && !state.playlistsLoading) item { EmptyState("Плейлистов пока нет", "Создай свой и добавляй треки через меню ⋮.") }
        items(state.playlists, key = { it.id }) { p ->
            RowCard(artworkUrl(p.coverArtwork ?: p.coverTrackId?.let { "/api/artwork/$it" }, 96), p.name,
                "${p.trackCount} треков · ${when(p.type) { "smart" -> "Умный"; "generated" -> "Подборка"; else -> "Свой" }}",
                onClick = { onOpen(p.id) })
        }
    }
    if (creating) PlaylistEditor(null, { creating = false }) { name, description -> creating = false; onCreate(name, description) }
}

@Composable
fun PlaylistDetailScreen(state: AppState, artworkUrl: (String?, Int) -> String?, onPlay: (Long, Int) -> Unit,
    onEdit: (Playlist, String, String) -> Unit, onDelete: (Long) -> Unit, onRemove: (Long, String) -> Unit,
    onMove: (Playlist, Int, Int) -> Unit, onAdd: () -> Unit, onRetry: () -> Unit, contentPadding: PaddingValues) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val p = state.playlist
    val busy = "playlist-mutation" in state.busy
    LazyColumn(contentPadding = contentPadding) {
        if (state.playlistLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (p == null && !state.playlistLoading) item {
            EmptyState("Не удалось открыть плейлист", "Проверь подключение и попробуй снова.")
            TextButton(onClick = onRetry, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Повторить") }
        }
        if (p != null) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(p.name, style = MaterialTheme.typography.headlineSmall)
                    if (p.description.isNotBlank()) Text(p.description, Modifier.padding(top = 8.dp))
                    Text("${p.trackCount} треков", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Button(onClick = { onPlay(p.id, 0) }, enabled = p.tracks.any { it.trackId > 0 && !it.unresolved }) {
                            Icon(Icons.Rounded.PlayArrow, null); Text("Слушать")
                        }
                        if (p.editable) IconButton(onClick = onAdd, enabled = !busy) { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, "Добавить треки") }
                        IconButton(onClick = { editing = true }, enabled = !busy) { Icon(Icons.Rounded.Edit, "Изменить название и описание") }
                        IconButton(onClick = { deleting = true }, enabled = !busy) { Icon(Icons.Rounded.DeleteOutline, "Удалить плейлист") }
                    }
                }
            }
            if (p.tracks.isEmpty()) item { EmptyState("В плейлисте пока пусто", "Добавляй треки из библиотеки.") }
            itemsIndexed(p.tracks, key = { _, t -> t.itemId }) { index, item ->
                var menu by remember { mutableStateOf(false) }
                val t = item.asTrack()
                RowCard(if (t.ready) artworkUrl(t.artworkPath, 96) else null, t.title ?: "Неизвестный трек",
                    listOfNotNull(t.artist, if (!t.ready) "Нет в библиотеке" else null).joinToString(" · "),
                    trailing = if (p.editable) ({
                        IconButton(onClick = { menu = true }, enabled = !busy) { Icon(Icons.Rounded.MoreVert, "Действия с треком плейлиста") }
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Выше") }, enabled = index > 0, onClick = { menu = false; onMove(p, index, -1) })
                            DropdownMenuItem(text = { Text("Ниже") }, enabled = index < p.tracks.lastIndex, onClick = { menu = false; onMove(p, index, 1) })
                            DropdownMenuItem(text = { Text("Убрать из плейлиста") }, onClick = { menu = false; onRemove(p.id, item.itemId) })
                        }
                    }) else null,
                    // Server indexes only resolved tracks. Imported unresolved rows cannot shift playback.
                    onClick = { if (t.ready) onPlay(p.id, p.tracks.take(index).count { it.trackId > 0 }) })
            }
        }
    }
    if (editing && p != null) PlaylistEditor(p, { editing = false }) { name, description -> editing = false; onEdit(p, name, description) }
    if (deleting && p != null) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Удалить «${p.name}»?") },
        text = { Text("Сам плейлист будет удалён. Музыкальные файлы останутся в библиотеке.") },
        confirmButton = { TextButton(onClick = { deleting = false; onDelete(p.id) }) { Text("Удалить") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Отмена") } })
}

@Composable
private fun PlaylistEditor(playlist: Playlist?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(playlist?.name.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(playlist?.description.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (playlist == null) "Новый плейлист" else "Изменить плейлист") },
        text = { Column {
            OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
            OutlinedTextField(description, { description = it }, label = { Text("Описание") }, maxLines = 4, modifier = Modifier.padding(top = 8.dp))
        } }, confirmButton = { TextButton(onClick = { onSave(name, description) }, enabled = name.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(playlists: List<Playlist>, busy: Boolean, onSelect: (Long) -> Unit, onCreate: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { SectionTitle("Добавить в плейлист") }
            if (playlists.none { it.editable }) item { EmptyState("Сначала создай плейлист", "Треки можно добавлять в собственные плейлисты.") }
            items(playlists.filter { it.editable }, key = { it.id }) { p ->
                ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("${p.trackCount} треков") },
                    trailingContent = { TextButton(onClick = { onSelect(p.id) }, enabled = !busy) { Text("Добавить") } })
            }
            item { TextButton(onClick = onCreate, enabled = !busy, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Создать плейлист") } }
        }
    }
}
