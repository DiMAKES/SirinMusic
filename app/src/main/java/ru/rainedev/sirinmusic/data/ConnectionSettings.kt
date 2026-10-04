package ru.rainedev.sirinmusic.data

interface ConnectionSettings {
    val baseUrl: String
    val token: String
    val clientId: String
}

data class ServerConnection(override val baseUrl: String, override val token: String, override val clientId: String) : ConnectionSettings
