package ru.rainedev.sirinmusic.data

import java.net.URI
import java.net.URLDecoder

/**
 * What the "add this server" QR in the musik web UI (Profile → Settings) carries:
 * `musik://connect?url=<server base url>&token=<API token>`. The same link opens the
 * app from the system camera. A QR with a plain http(s) address only fills the address.
 * An invitation link from a server with accounts (`https://host/?invitation=<secret>`)
 * gives the address and the invitation for creating an account.
 */
data class ConnectLink(val baseUrl: String, val token: String?, val invitation: String? = null) {
    companion object {
        fun parse(raw: String): ConnectLink? {
            val text = raw.trim()
            val uri = runCatching { URI(text) }.getOrNull() ?: return null
            return when (uri.scheme?.lowercase()) {
                "musik" -> {
                    if (uri.host?.lowercase() != "connect") return null
                    val query = parseQuery(uri.rawQuery)
                    val url = query["url"]?.trim()?.trimEnd('/') ?: return null
                    if (validateServerUrl(url) != null) return null
                    ConnectLink(url, query["token"]?.trim()?.takeIf { it.isNotEmpty() },
                        query["invitation"]?.trim()?.takeIf { it.isNotEmpty() })
                }
                "http", "https" -> {
                    val url = text.substringBefore('#').substringBefore('?').trimEnd('/')
                    val invitation = parseQuery(uri.rawQuery)["invitation"]?.trim()?.takeIf { it.isNotEmpty() }
                    if (validateServerUrl(url) != null) null else ConnectLink(url, null, invitation)
                }
                else -> null
            }
        }

        // The server encodes with Go's url.Values: space as "+", a literal "+" as "%2B",
        // which is exactly what URLDecoder expects.
        private fun parseQuery(raw: String?): Map<String, String> =
            raw.orEmpty().split('&').mapNotNull { pair ->
                val key = pair.substringBefore('=', "")
                if (key.isEmpty()) return@mapNotNull null
                // The Charset overload is API 33+; minSdk is 27.
                runCatching {
                    URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(pair.substringAfter('='), "UTF-8")
                }.getOrNull()
            }.toMap()
    }
}
