package net.thevenot.comwatt.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TempoApiClientTest {
    private fun clientReturning(body: String) = HttpClient(MockEngine { _ ->
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    @Test
    fun dayColorParsesCodeJour() = runTest {
        val api = TempoApiClient(clientReturning("""{"dateJour":"2026-07-01","codeJour":3}"""))
        val result = api.dayColor(LocalDate(2026, 7, 1))
        assertTrue(result.isRight())
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun tarifsParsesAllSixRates() = runTest {
        val body = """{"bleuHC":0.1296,"bleuHP":0.1609,"blancHC":0.1486,"blancHP":0.1894,"rougeHC":0.1568,"rougeHP":0.7562}"""
        val api = TempoApiClient(clientReturning(body))
        val result = api.tarifs()
        assertEquals(0.7562, result.getOrNull()?.rougeHP)
    }
}
