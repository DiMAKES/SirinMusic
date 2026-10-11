package ru.rainedev.sirinmusic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.rainedev.sirinmusic.data.*
import ru.rainedev.sirinmusic.ui.theme.SirinMusicTheme

class ServerSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // This screen can reveal a server-wide credential; exclude it from screenshots and recents.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val app = application as SirinApp
        setContent {
            val appearance by app.settings.appearance.collectAsStateWithLifecycle()
            SirinMusicTheme(appearance) { ServerSettingsScreen(app, ::finish) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerSettingsScreen(app: SirinApp, onBack: () -> Unit) {
    var reply by remember { mutableStateOf<ServerSettingsReply?>(null) }
    var addr by remember { mutableStateOf("") }
    var publicUrl by remember { mutableStateOf("") }
    var library by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8787") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var reveal by remember { mutableStateOf(false) }
    var ip by remember { mutableStateOf("") }
    var listing by remember { mutableStateOf<ServerDirs?>(null) }
    var restartDialog by remember { mutableStateOf(false) }
    var qr by remember { mutableStateOf<Bitmap?>(null) }
    var qrUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun install(value: ServerSettingsReply) {
        reply = value; addr = value.saved.addr; publicUrl = value.saved.publicUrl; library = value.saved.library
        host = addr.substringBeforeLast(':').removeSurrounding("[", "]")
        port = addr.substringAfterLast(':', "8787")
        mode = when (host) { "", "0.0.0.0", "::" -> if (publicUrl.isBlank()) 0 else 1; "127.0.0.1", "localhost", "::1" -> 2; else -> 3 }
        reveal = false; qr = null
    }
    fun action(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true; error = null
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = when ((e as? ApiException)?.code) {
                403 -> "Нет доступа к настройкам сервера. Токены устройств в режиме аккаунтов не дают права администратора. Открой настройки в браузере с учётной записью владельца."
                404 -> "Эта версия сервера не поддерживает настройки через API."
                else -> e.message ?: "Не удалось связаться с сервером"
            } }
            finally { busy = false }
        }
    }
    fun copy(text: String, secret: Boolean = false) {
        val clip = ClipData.newPlainText(if (secret) "API-токен" else "Адрес", text)
        if (secret) clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        app.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        notice = "Скопировано"
    }
    LaunchedEffect(Unit) { action { install(app.api.serverSettings()) } }
    Scaffold(topBar = { TopAppBar(title = { Text("Настройки сервера") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            notice?.let { Text(it) }
            val data = reply
            if (data == null) {
                Button(onClick = { action { install(app.api.serverSettings()) } }, enabled = !busy) { Text("Загрузить настройки") }
            } else {
                if (data.restartPending) Text("Изменения сохранены и заработают после перезапуска сервера.")
                Button(onClick = { restartDialog = true }, enabled = !busy && data.restartSupported) { Text("Перезапустить сервер") }
                if (!data.restartSupported) Text("Эта установка не поддерживает перезапуск из приложения. После сохранения перезапусти сервер на компьютере; для Docker проверь также постоянную конфигурацию контейнера.")
                Text("Адрес сервера", style = MaterialTheme.typography.titleLarge)
                listOf("Вся домашняя сеть", "Интернет", "Только этот компьютер", "Только адрес").forEachIndexed { index, label ->
                    Row { RadioButton(selected = mode == index, enabled = !busy, onClick = { mode = index }); TextButton(onClick = { mode = index }, enabled = !busy) { Text(label) } }
                }
                if (mode == 3) OutlinedTextField(host, { host = it }, label = { Text("IP-адрес на сервере") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(port, { port = it }, label = { Text("Порт") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(publicUrl, { publicUrl = it }, label = { Text("Внешний адрес: https://домен или IP") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (mode == 1) Text("Для доступа извне настрой Tailscale, Cloudflare Tunnel или проброс TCP-порта на роутере. Рекомендуются HTTPS и пароль. Для HTTPS на сервере установи MUSIK_SECURE_COOKIE=1. Выбор режима сам не открывает доступ из интернета.")
                if (!data.phone.authEnabled) Text("Пароль выключен. Не открывай сервер в интернет без защиты.", color = MaterialTheme.colorScheme.error)
                Text("Папка с музыкой", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(library, { library = it }, label = { Text("Путь на сервере") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { action { listing = app.api.serverDirs(library) } }, enabled = !busy) { Text("Выбрать папку…") }
                Text("После смены папки требуется отдельное пересканирование. Треки из старой папки пропадут из библиотеки, история останется. Сохранение не запускает пересканирование.")
                Button(onClick = { action {
                    val number = port.toIntOrNull(); require(number != null && number in 1..65535) { "Порт должен быть от 1 до 65535" }
                    val listenHost = when (mode) { 0, 1 -> ""; 2 -> "127.0.0.1"; else -> host.trim().also { require(it.isNotEmpty()) { "Введи IP-адрес" } } }
                    val listen = if (listenHost.contains(':')) "[$listenHost]:$number" else "$listenHost:$number"
                    install(app.api.saveServerSettings(ServerValues(listen, publicUrl.trim(), library.trim())))
                    notice = "Сохранено. Адрес подключения приложения остаётся прежним; при смене адреса измени его в разделе «Подключение»."
                } }, enabled = !busy) { Text("Сохранить") }
                Text(data.envPath, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Подключение телефона", style = MaterialTheme.typography.titleLarge)
                data.phone.urls.forEach { url ->
                    Text(url)
                    Row {
                        TextButton(onClick = { copy(url) }) { Text("Копировать") }
                        TextButton(onClick = { action {
                            val payload = "musik://connect?url=${java.net.URLEncoder.encode(url, "UTF-8")}&token=${java.net.URLEncoder.encode(data.phone.token, "UTF-8")}"
                            qr = withContext(Dispatchers.Default) {
                                val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 768, 768)
                                Bitmap.createBitmap(768, 768, Bitmap.Config.ARGB_8888).apply {
                                    val pixels = IntArray(768 * 768) { i -> if (matrix[i % 768, i / 768]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
                                    setPixels(pixels, 0, 768, 0, 0, 768, 768)
                                }
                            }; qrUrl = url
                        } }, enabled = !busy) { Text("Показать QR") }
                    }
                }
                if (data.phone.urls.isEmpty()) Text("Нет адресов для телефона. Проверь режим сети и внешний адрес.")
                OutlinedTextField(data.phone.token, {}, readOnly = true, label = { Text("API-токен") }, visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Row { TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Скрыть" else "Показать") }; TextButton(onClick = { copy(data.phone.token, true) }, enabled = data.phone.token.isNotEmpty()) { Text("Копировать") } }
                Text("QR-код содержит адрес и API-токен. Передавай его только своим устройствам.")
                Text("Внешний IP", style = MaterialTheme.typography.titleLarge)
                if (ip.isNotEmpty()) { Text(ip); TextButton(onClick = { copy(ip) }) { Text("Копировать") } }
                OutlinedButton(onClick = { action { ip = app.api.serverExternalIp().ip } }, enabled = !busy) { Text(if (ip.isEmpty()) "Узнать внешний IP" else "Обновить") }
                Text("Это адрес роутера. При сером IP проброс порта не поможет: используй Tailscale или Cloudflare Tunnel.")
                TextButton(onClick = { action { app.startActivity(Intent(Intent.ACTION_VIEW, app.api.url(data.phone.openapi).toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }, enabled = !busy) { Text("Описание API: openapi.json") }
            }
        }
    }
    listing?.let { dirs ->
        AlertDialog(onDismissRequest = { listing = null }, title = { Text("Папка на сервере") }, text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Text(dirs.path.ifEmpty { "Корневые папки" })
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (dirs.parent.isNotEmpty()) TextButton(onClick = { action { listing = app.api.serverDirs(dirs.parent) } }, enabled = !busy) { Text("На уровень выше") }
                (dirs.dirs ?: emptyList()).forEach { dir -> TextButton(onClick = { action { listing = app.api.serverDirs(dir.path) } }, enabled = !busy) { Text(dir.name) } }
            }
        }, confirmButton = { TextButton(onClick = { library = dirs.path; listing = null }, enabled = !busy && dirs.path.isNotEmpty()) { Text("Выбрать эту папку") } }, dismissButton = { TextButton(onClick = { listing = null }) { Text("Отмена") } })
    }
    if (restartDialog) AlertDialog(onDismissRequest = { restartDialog = false }, title = { Text("Перезапустить сервер?") }, text = { Text("Подключения временно прервутся. Сохранённые настройки применятся после запуска.") }, confirmButton = {
        TextButton(onClick = { restartDialog = false; action { app.api.restartServer(); notice = "Команда перезапуска отправлена. Подключение временно прервётся." } }) { Text("Перезапустить") }
    }, dismissButton = { TextButton(onClick = { restartDialog = false }) { Text("Отмена") } })
    qr?.let { bitmap -> AlertDialog(onDismissRequest = { qr = null }, title = { Text("Подключить приложение") }, text = {
        Column { Text(qrUrl); Image(bitmap.asImageBitmap(), "QR-код подключения", Modifier.fillMaxWidth().aspectRatio(1f)) }
    }, confirmButton = { TextButton(onClick = { qr = null }) { Text("Закрыть") } }) }
}
