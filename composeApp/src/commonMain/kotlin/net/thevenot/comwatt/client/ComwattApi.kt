package net.thevenot.comwatt.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import net.thevenot.comwatt.model.Site
import net.thevenot.comwatt.model.SiteTimeSeries
import net.thevenot.comwatt.model.User
import net.thevenot.comwatt.utils.toZoneString

class ComwattApi(val client: HttpClient) {
    suspend fun authenticate(email: String, password: Password): Session? {
        val url = "$BASE_URL/v1/authent"
        val encodedPassword = password.encodedValue
        val data = AuthRequest(email, encodedPassword)
        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(data)
        }

        val setCookieHeader = response.headers["set-cookie"]
        val sessionToken = setCookieHeader?.substringAfter("cwt_session=")?.substringBefore(";")
        val expiresString = response.headers["x-cwt-token"]
        val expires = expiresString?.let { Instant.parse(it).toLocalDateTime(TimeZone.UTC) }

        return sessionToken?.let { token ->
            expires?.let { exp ->
                Session(token, exp)
            }
        }
    }

    suspend fun sites(sessionToken: String): List<Site> {
        return client.get("$BASE_URL/sites") {
            header("Cookie", "cwt_session=$sessionToken")
        }.body()
    }

    suspend fun fetchSiteTimeSeries(sessionToken: String, siteId: Int, startTime: Instant = Clock.System.now().minus(5, DateTimeUnit.MINUTE)): SiteTimeSeries {
        val endTime = Clock.System.now()

        val timeZone = TimeZone.of("Europe/Paris")
        return client.get("$BASE_URL/aggregations/site-time-series") {
            header("Cookie", "cwt_session=$sessionToken")
            parameter("id", siteId)
            parameter("measureKind", "FLOW")
            parameter("aggregationLevel", "NONE")
            parameter("start", startTime.toZoneString(timeZone))
            parameter("end", endTime.toZoneString(timeZone))
        }.body()
    }

    suspend fun authenticated(sessionToken: String) : User? {
        return client.get("$BASE_URL/users/authenticated") {
            header("Cookie", "cwt_session=$sessionToken")
        }.body()
    }
}

@Serializable
data class AuthRequest(val username: String, val password: String)