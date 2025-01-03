package net.thevenot.comwatt.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

expect fun engine(): HttpClientEngine

fun createClient() = HttpClient(engine()) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(
            json = Json {
                encodeDefaults = true
                isLenient = true
                allowSpecialFloatingPointValues = true
                allowStructuredMapKeys = true
                prettyPrint = false
                useArrayPolymorphism = false
                ignoreUnknownKeys = true
            },
        )
    }
    install(DefaultRequest) {
        apply {
            url {
                protocol = URLProtocol.HTTPS
                host = "energy.comwatt.com"
            }
        }
    }
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.ALL
    }
}

@Serializable
data class Session(val token: String, val expires: LocalDateTime)