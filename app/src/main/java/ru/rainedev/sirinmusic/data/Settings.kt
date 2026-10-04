package ru.rainedev.sirinmusic.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Базовый URL и API-токен сервера. Токен — секрет, поэтому лежит в
 * EncryptedSharedPreferences (ключ в Keystore), а не в обычных префах.
 */
class Settings(context: Context) : ConnectionSettings {

    private val prefs: SharedPreferences = run {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "sirin_secure",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override var baseUrl: String
        get() = prefs.getString(KEY_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_URL, value.trim().trimEnd('/')).apply()

    override var token: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TOKEN, value.trim()).apply()

    private val appearancePrefs = context.getSharedPreferences("sirin_appearance", Context.MODE_PRIVATE)
    private val _appearance = MutableStateFlow(Appearance(
        theme = runCatching { ThemeMode.valueOf(appearancePrefs.getString("theme", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM),
        dynamicColor = appearancePrefs.getBoolean("dynamic_color", true),
        palette = runCatching { Palette.valueOf(appearancePrefs.getString("palette", "SIRIN")!!) }.getOrDefault(Palette.SIRIN),
    ))
    val appearance = _appearance.asStateFlow()

    fun setAppearance(value: Appearance) {
        appearancePrefs.edit().putString("theme", value.theme.name)
            .putBoolean("dynamic_color", value.dynamicColor).putString("palette", value.palette.name).apply()
        _appearance.value = value
    }

    fun setConnection(url: String, apiToken: String) {
        prefs.edit().putString(KEY_URL, url.trim().trimEnd('/')).putString(KEY_TOKEN, apiToken.trim()).apply()
    }

    /** Один и тот же id во всех событиях — по нему сервер отличает устройства. */
    override val clientId: String
        get() = prefs.getString(KEY_CLIENT, null) ?: java.util.UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_CLIENT, it).apply()
        }

    val isConfigured: Boolean get() = baseUrl.isNotEmpty() && token.isNotEmpty()

    fun clear() = prefs.edit().remove(KEY_URL).remove(KEY_TOKEN).apply()

    private companion object {
        const val KEY_URL = "base_url"
        const val KEY_TOKEN = "api_token"
        const val KEY_CLIENT = "client_id"
    }
}


enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Palette { SIRIN, FOREST, OCEAN, SUNSET }
data class Appearance(val theme: ThemeMode = ThemeMode.SYSTEM, val dynamicColor: Boolean = true, val palette: Palette = Palette.SIRIN)
