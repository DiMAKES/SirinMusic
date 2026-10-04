package ru.rainedev.sirinmusic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.rainedev.sirinmusic.data.Profile

@Composable
fun ProfileScreen(
    serverUrl: String,
    serverTracks: Int,
    serverVersion: String?,
    maturity: String?,
    profile: Profile?,
    favorites: Int,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    contentPadding: PaddingValues,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onFavorites: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
    ) {
        Text("Профиль", style = MaterialTheme.typography.headlineSmall)

        Row2("Сервер", serverUrl)
        Row2("Версия", serverVersion ?: "—")
        Row2("Треков в библиотеке", serverTracks.toString())
        Row2("Профиль вкуса", maturity?.let { maturityRu(it) } ?: "—")
        Row2("В избранном", favorites.toString())
        if (profile?.plays != null) Row2("Прослушано", profile.plays.toString())
        if (profile?.likes != null) Row2("Лайков", profile.likes.toString())
        if (profile?.skips != null) Row2("Пропусков", profile.skips.toString())

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Настройки") }
        OutlinedButton(onClick = onFavorites, modifier = Modifier.fillMaxWidth()) { Text("Открыть избранное") }
        OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) { Text("О разработчике") }
        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Обновить данные")
        }
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text("Выйти и забыть токен")
        }
    }
}

@Composable
private fun Row2(label: String, value: String) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
