package net.thevenot.comwatt.utils

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun configureMockEngineWithError(statusCode: HttpStatusCode = HttpStatusCode.InternalServerError): MockEngine =
    MockEngine {
        respondError(statusCode, "Some error occurred")
    }

fun configureMockEngine(
    expectedResponseBody: String,
    url: Url,
    httpMethod: HttpMethod = HttpMethod.Post,
    expectedContentType: ContentType? = ContentType.Application.Json
): MockEngine =
    MockEngine { request ->
        assertEquals(url, request.url)
        assertEquals(httpMethod, request.method)

        respond(
            content = expectedResponseBody,
            status = HttpStatusCode.OK,
            headers = expectedContentType?.let { headersOf(HttpHeaders.ContentType, it.toString()) } ?: headersOf()
        )
    }

fun configureMockEngineEmptyContent(
    role: String? = null,
    language: String? = null,
    httpMethod: HttpMethod = HttpMethod.Post,
    responseStatus: HttpStatusCode = HttpStatusCode.OK
): MockEngine =
    MockEngine { request ->
        assertEquals(httpMethod, request.method)
        role?.let { assertEquals(it, request.headers["x-expense-selected-role"]) }
        language?.let { assertEquals(it, request.headers["x-expense-selected-language"]) }
        assertTrue(request.body is EmptyContent)

        respond(
            content = "",
            status = responseStatus,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )
    }