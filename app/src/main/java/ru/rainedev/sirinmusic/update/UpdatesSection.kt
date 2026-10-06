package ru.rainedev.sirinmusic.update

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import kotlinx.coroutines.launch
import ru.rainedev.sirinmusic.BuildConfig
import ru.rainedev.sirinmusic.SirinApp

@Composable
internal fun UpdatesSection(app: SirinApp) {
    val state by app.updates.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installHint by remember { mutableStateOf<String?>(null) }
    var permissionGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
        if (it) app.updates.notifyAvailable()
    }
    fun launchInstaller(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            installHint = null
        } catch (_: Exception) { installHint = "Не удалось открыть системный установщик" }
    }
    val installPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val file = app.updates.state.value.ready
        if (context.packageManager.canRequestPackageInstalls() && file?.exists() == true) launchInstaller(file)
        else installHint = "Разреши установку обновлений для Sirin Music и нажми «Установить»."
    }
    fun install(file: File) {
        if (context.packageManager.canRequestPackageInstalls()) launchInstaller(file)
        else try {
            installPermission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
        } catch (_: Exception) { installHint = "Открой настройки Android и разреши Sirin Music устанавливать приложения." }
    }
    Column(Modifier.fillMaxWidth()) {
        Text("Обновления", style = MaterialTheme.typography.titleLarge)
        Text("Установлена версия ${BuildConfig.VERSION_NAME}", Modifier.padding(top = 12.dp))
        Text("Стабильные релизы с GitHub. Установка — после твоего подтверждения.",
            style = MaterialTheme.typography.bodySmall)
        ListItem(headlineContent = { Text("Проверять обновления при запуске приложения") }, supportingContent = {
            Text("Один раз при запуске; возврат из фона и поворот экрана не запускают повторную проверку.")
        }, trailingContent = { Checkbox(state.onLaunch, onCheckedChange = app.updates::setOnLaunch) })
        ListItem(headlineContent = { Text("Проверять обновления в фоне") }, supportingContent = {
            Text("Раз в сутки; уведомление о новой версии. APK автоматически не скачивается.")
        }, trailingContent = { Switch(state.automatic, onCheckedChange = app.updates::setAutomatic) })
        if ((state.automatic || state.onLaunch) && !permissionGranted && Build.VERSION.SDK_INT >= 33) {
            TextButton(onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Разрешить уведомления") }
        }
        OutlinedButton(enabled = !state.checking && !state.downloading, onClick = {
            installHint = null
            scope.launch { app.updates.check(notify = true) }
        }) { Text(if (state.checking) "Проверяю…" else "Проверить новую версию") }
        if (state.checking) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.available == null && state.checkedAt > 0 && !state.checking && state.error == null) {
            Text("Новых стабильных версий нет", style = MaterialTheme.typography.bodyMedium)
        }
        state.available?.let { update ->
            Text("Доступна версия ${update.version}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.page))) }) { Text("Что нового на GitHub") }
            if (state.downloading) {
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Text("Загрузка: ${(state.progress * 100).toInt()}%")
            }
            Button(enabled = !state.downloading && !state.checking, onClick = {
                scope.launch {
                    val file = state.ready?.takeIf { it.exists() } ?: app.updates.download()
                    if (file != null) install(file)
                }
            }) { Text(if (state.ready?.exists() == true) "Установить обновление" else "Скачать и обновить") }
        }
        (state.error ?: installHint)?.let { Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
    }
}
