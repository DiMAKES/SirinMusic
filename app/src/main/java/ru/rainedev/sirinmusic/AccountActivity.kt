package ru.rainedev.sirinmusic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.*
import ru.rainedev.sirinmusic.data.*
import ru.rainedev.sirinmusic.ui.theme.SirinMusicTheme

class AccountSession(baseUrl: String) : ViewModel() {
    val api = runCatching { AccountApi(baseUrl) }.getOrNull()
    override fun onCleared() {
        api?.let { client -> CoroutineScope(Dispatchers.IO).launch { try { runCatching { client.logout() } } finally { client.close() } } }
    }
}
class AccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val app = application as SirinApp
        val session = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AccountSession(app.settings.baseUrl) as T
        })[AccountSession::class.java]
        setContent {
            val appearance by app.settings.appearance.collectAsStateWithLifecycle()
            SirinMusicTheme(appearance) { AccountScreen(app, session.api, ::finish) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountScreen(app: SirinApp, api: AccountApi?, onBack: () -> Unit) {
    var me by remember { mutableStateOf<AccountMe?>(null) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newProfile by remember { mutableStateOf("") }
    var profileName by remember { mutableStateOf("") }
    var devices by remember { mutableStateOf<List<ManagedDevice>>(emptyList()) }
    var users by remember { mutableStateOf<List<ManagedUser>>(emptyList()) }
    var invites by remember { mutableStateOf<List<ManagedInvitation>>(emptyList()) }
    var deviceName by remember { mutableStateOf("Телефон") }
    var deviceProfile by remember { mutableStateOf("") }
    var issuedDevice by remember { mutableStateOf<IssuedDevice?>(null) }
    var issuedInvite by remember { mutableStateOf<IssuedInvitation?>(null) }
    var email by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("24") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<Pair<String, suspend () -> Unit>?>(null) }
    var qr by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var phoneUrl by remember { mutableStateOf(api?.origin.orEmpty()) }
    var revealToken by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun reload() {
        val client = api ?: return
        val account = client.me()
        me = account.takeIf { it.ok && it.multiUser }
        if (me == null) return
        profileName = account.activeProfile?.name.orEmpty()
        if (account.profiles.none { it.id == deviceProfile }) deviceProfile = account.activeProfile?.id.orEmpty()
        devices = client.devices()
        if ("admin" in account.user.roles) {
            users = client.users(); invites = client.invitations()
            phoneUrl = client.settings().phone.urls.firstOrNull() ?: client.origin
        } else { users = emptyList(); invites = emptyList(); phoneUrl = client.origin }
    }
    fun action(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true; error = null; notice = null
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Не удалось выполнить запрос"; if ((e as? ApiException)?.code == 401) { me = null; issuedDevice = null; issuedInvite = null; qr = null } }
            finally { busy = false }
        }
    }
    fun copy(text: String, secret: Boolean = false) {
        val clip = ClipData.newPlainText("Sirin Music", text)
        if (secret) clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        app.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip); notice = "Скопировано"
    }
    LaunchedEffect(api) { if (api?.base?.isHttps == true) action { reload() } }
    Scaffold(topBar = { TopAppBar(title = { Text("Аккаунт и профили") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; notice?.let { Text(it) }
            if (api == null || !api.base.isHttps) {
                Text("Управление аккаунтами требует HTTPS. Укажи HTTPS-адрес сервера в настройках подключения.")
            } else if (me == null) {
                Text("Вход для управления аккаунтом", style = MaterialTheme.typography.titleLarge)
                Text("Токен плеера не даёт права управлять профилями и устройствами. Введи имя и пароль; пароль не сохраняется.")
                OutlinedTextField(username, { username = it }, label = { Text("Имя") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Пароль") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { action { api.login(username, password); password = ""; reload() } }, enabled = !busy) { Text("Войти") }
            } else {
                val account = me!!
                Text(account.user.displayName, style = MaterialTheme.typography.headlineSmall)
                Text("У каждого профиля свои история, любимые треки, плейлисты и настройки радио.")
                if (account.localAccount) {
                    Text("Сменить пароль", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(currentPassword, { currentPassword = it }, label = { Text("Текущий пароль") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(newPassword, { newPassword = it }, label = { Text("Новый пароль") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { action { api.password(currentPassword, newPassword); currentPassword = ""; newPassword = ""; notice = "Пароль изменён" } }, enabled = !busy && newPassword.length in 8..256 && currentPassword.isNotEmpty()) { Text("Сменить пароль") }
                }
                HorizontalDivider(); Text("Профили", style = MaterialTheme.typography.titleLarge)
                account.profiles.forEach { profile ->
                    Row { RadioButton(selected = profile.id == account.activeProfile?.id, enabled = !busy, onClick = { action { api.activateProfile(profile.id); reload() } }); TextButton(onClick = { action { api.activateProfile(profile.id); reload() } }, enabled = !busy) { Text(profile.name + if (profile.isDefault) " · основной" else "") } }
                }
                Text("Этот выбор меняет профиль сеанса управления. Чтобы переключить плеер, нажми «Использовать профиль в приложении».")
                OutlinedButton(onClick = { action {
                    val profile = account.activeProfile ?: kotlin.error("Выбери профиль")
                    val issue = api.createDevice("Sirin Music Android", profile.id)
                    MusikApi(ServerConnection(api.origin, issue.token, app.settings.clientId)).validateConnection()
                    app.playback.stop(); app.settings.setConnection(api.origin, issue.token)
                    devices = api.devices(); notice = "Плеер переключён на ${profile.name}. Предыдущий токен можно отозвать в списке устройств."
                } }, enabled = !busy && account.activeProfile != null) { Text("Использовать профиль в приложении") }
                OutlinedTextField(newProfile, { newProfile = it }, label = { Text("Новый профиль") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { action { api.createProfile(newProfile); newProfile = ""; reload() } }, enabled = !busy && newProfile.isNotBlank()) { Text("Создать профиль") }
                OutlinedTextField(profileName, { profileName = it }, label = { Text("Название активного профиля") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row {
                    TextButton(onClick = { action { api.renameProfile(account.activeProfile!!.id, profileName); reload() } }, enabled = !busy && account.activeProfile != null && profileName.isNotBlank()) { Text("Переименовать") }
                    TextButton(onClick = { confirmation = "Удалить профиль? Его персональные данные и токены будут удалены. Библиотека музыки общая и останется." to { api.deleteProfile(account.activeProfile!!.id); reload() } }, enabled = !busy && account.activeProfile?.isDefault == false) { Text("Удалить профиль") }
                }
                HorizontalDivider(); Text("Устройства и токены", style = MaterialTheme.typography.titleLarge)
                Text("Токен действует 180 дней и привязан к выбранному профилю. Его можно скопировать в поле токена или передать другому устройству через QR.")
                OutlinedTextField(deviceName, { deviceName = it }, label = { Text("Имя устройства") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                account.profiles.forEach { profile -> Row { RadioButton(selected = deviceProfile == profile.id, enabled = !busy, onClick = { deviceProfile = profile.id }); TextButton(onClick = { deviceProfile = profile.id }, enabled = !busy) { Text(profile.name + if (profile.isDefault) " · основной" else "") } } }
                Button(onClick = { action { issuedDevice = api.createDevice(deviceName, deviceProfile); revealToken = false; devices = api.devices() } }, enabled = !busy && deviceName.isNotBlank() && deviceProfile.isNotBlank()) { Text("Создать токен") }
                issuedDevice?.let { issued ->
                    Text("Новый токен показывается только сейчас. Скопируй его или покажи QR.")
                    OutlinedTextField(issued.token, {}, readOnly = true, label = { Text("API-токен") }, visualTransformation = if (revealToken) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    Row {
                        TextButton(onClick = { revealToken = !revealToken }) { Text(if (revealToken) "Скрыть" else "Показать") }
                        TextButton(onClick = { copy(issued.token, true) }) { Text("Копировать") }
                        TextButton(onClick = { action { qr = createConnectionQr(phoneUrl, issued.token) } }, enabled = !busy) { Text("QR") }
                    }
                }
                devices.forEach { device ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                        Text(device.name, style = MaterialTheme.typography.titleMedium)
                        Text("Профиль: ${device.profileName}"); Text("Создан: ${accountDate(device.createdAt)}")
                        Text("Последнее использование: ${device.lastUsedAt?.let(::accountDate) ?: "Ещё не использован"}")
                        Text("Действует до: ${accountDate(device.expiresAt)}")
                        if (device.revokedAt != null) Text("Отозван") else Row {
                            TextButton(onClick = { confirmation = "Перевыпустить токен ${device.name}? Прежний токен перестанет работать." to { issuedDevice = api.replaceDevice(device.id); revealToken = false; devices = api.devices() } }, enabled = !busy) { Text("Перевыпустить") }
                            TextButton(onClick = { confirmation = "Отозвать токен ${device.name}? Устройство потеряет доступ, в том числе этот телефон, если он использует этот токен." to { api.revokeDevice(device.id); devices = api.devices() } }, enabled = !busy) { Text("Отозвать") }
                        }
                    } }
                }
                if ("admin" in account.user.roles) {
                    HorizontalDivider(); Text("Пригласить пользователя", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(email, { email = it }, label = { Text("Email (необязательно)") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(hours, { hours = it }, label = { Text("Срок в часах") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { action { issuedInvite = api.invite(email, hours.toIntOrNull() ?: kotlin.error("Введи срок от 1 до 168 часов")); invites = api.invitations() } }, enabled = !busy) { Text("Создать приглашение") }
                    issuedInvite?.let { invite -> Text(invite.url); TextButton(onClick = { copy(invite.url, true) }) { Text("Копировать приглашение") } }
                    Text("Пользователи", style = MaterialTheme.typography.titleLarge)
                    users.forEach { user -> key(user.id, user.status, user.admin) {
                        var active by remember { mutableStateOf(user.status == "active") }; var admin by remember { mutableStateOf(user.admin) }
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                            Text(user.displayName, style = MaterialTheme.typography.titleMedium); Text("${user.id} · профилей: ${user.profiles}")
                            Row { Checkbox(active, { active = it }, enabled = !busy); Text("Активен") }
                            Row { Checkbox(admin, { admin = it }, enabled = !busy); Text("Администратор") }
                            Button(onClick = { confirmation = "Сохранить права ${user.displayName}? Отключение пользователя отзовёт его сеансы и токены." to { api.updateUser(user.id, active, admin); reload() } }, enabled = !busy && (active != (user.status == "active") || admin != user.admin)) { Text("Сохранить") }
                        } }
                    } }
                    Text("Приглашения", style = MaterialTheme.typography.titleLarge)
                    invites.forEach { invite -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                        Text(invite.email.ifBlank { "Без привязки к email" })
                        Text("${when { invite.revokedAt.isNotBlank() -> "Отозвано"; invite.consumedAt.isNotBlank() -> "Использовано"; runCatching { java.time.Instant.parse(invite.expiresAt).isBefore(java.time.Instant.now()) }.getOrDefault(false) -> "Истекло"; else -> "Активно" }} · до ${accountDate(invite.expiresAt)}")
                        if (invite.revokedAt.isBlank() && invite.consumedAt.isBlank()) TextButton(onClick = { confirmation = "Отозвать приглашение?" to { api.revokeInvitation(invite.id); invites = api.invitations() } }, enabled = !busy) { Text("Отозвать") }
                    } } }
                }
                HorizontalDivider()
                TextButton(onClick = { action { reload() } }, enabled = !busy) { Text("Обновить") }
                TextButton(onClick = { action { api.logout(); me = null; issuedDevice = null; issuedInvite = null; qr = null; password = ""; currentPassword = ""; newPassword = "" } }, enabled = !busy) { Text("Завершить сеанс управления") }
            }
        }
    }
    confirmation?.let { (text, operation) -> AlertDialog(onDismissRequest = { confirmation = null }, title = { Text("Подтверждение") }, text = { Text(text) }, confirmButton = { TextButton(onClick = { confirmation = null; action { operation() } }) { Text("Подтвердить") } }, dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Отмена") } }) }
    qr?.let { bitmap -> AlertDialog(onDismissRequest = { qr = null }, title = { Text("Подключение устройства") }, text = { Column { Text(phoneUrl); Image(bitmap.asImageBitmap(), "QR подключения", Modifier.fillMaxWidth().aspectRatio(1f)); Text("QR содержит личный токен. Передавай только своему устройству.") } }, confirmButton = { TextButton(onClick = { qr = null }) { Text("Закрыть") } }) }
}

private fun accountDate(value: String): String = runCatching {
    java.time.Instant.parse(value).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss"))
}.getOrDefault(value)
private suspend fun createConnectionQr(url: String, token: String): android.graphics.Bitmap = withContext(Dispatchers.Default) {
    val payload = "musik://connect?url=${java.net.URLEncoder.encode(url, "UTF-8")}&token=${java.net.URLEncoder.encode(token, "UTF-8")}"
    val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(payload, com.google.zxing.BarcodeFormat.QR_CODE, 768, 768)
    androidx.core.graphics.createBitmap(768, 768).apply {
        setPixels(IntArray(768*768) { i -> if (matrix[i%768,i/768]) android.graphics.Color.BLACK else android.graphics.Color.WHITE },0,768,0,0,768,768)
    }
}
