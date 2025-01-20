package net.thevenot.comwatt.client

import arrow.core.Either
import arrow.core.flatMap
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.addCookie
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.http.setCookie
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.DeviceDto
import net.thevenot.comwatt.model.SiteDto
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.UserDto
import net.thevenot.comwatt.model.safeRequest
import net.thevenot.comwatt.utils.toZoneString
import kotlin.time.Duration

class ComwattApi(val client: HttpClient, val baseUrl: String) {
    suspend fun authenticate(
        email: String,
        password: Password
    ): Either<ApiError.GenericError, Unit> {
        val url = "$baseUrl/v1/authent"
        val encodedPassword = password.encodedValue
        val data = AuthRequest(email, encodedPassword)
        val response: Either<ApiError.GenericError, HttpResponse> =
            withContext(Dispatchers.IO) {
            try {
                Either.Right(client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(data)
                })
            } catch (e: Exception) {
                Either.Left(ApiError.GenericError(e.message, "Something went wrong"))
            }
        }

        return response.flatMap { resp ->
            val sessionToken = response.getOrNull()?.setCookie()?.first()?.value
            val expiresString = resp.headers["x-cwt-token"]
            val expires = expiresString?.let { expires ->
                response.getOrNull()?.setCookie()?.first()
                    ?.copy(expires = GMTDate(Instant.parse(expires).minus(Duration.parse("58m")).toEpochMilliseconds()))
                    ?.let {
                        cookiesStorage.addCookie("https://energy.comwatt.com/", it)
                    }
                Instant.parse(expires).toLocalDateTime(TimeZone.UTC)
            }

            if (sessionToken != null && expires != null) {
                Either.Right(Unit)
            } else {
                Either.Left(ApiError.GenericError("Invalid session data", "Session token or expiration date is missing"))
            }
        }
    }

    suspend fun sites(): Either<ApiError, List<SiteDto>> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("api/sites")
                }
            }
        }
    }

    suspend fun fetchSiteTimeSeries(
        siteId: Int,
        startTime: Instant = Clock.System.now().minus(5, DateTimeUnit.MINUTE)
    ): Either<ApiError, SiteTimeSeriesDto> {
        val endTime = Clock.System.now()

        val timeZone = TimeZone.of("Europe/Paris")
        return withContext(Dispatchers.IO) {
            val cookies = client.cookies("https://energy.comwatt.com/")
            Napier.d { "cookies $cookies" }

            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("api/aggregations/site-time-series")
                    parameter("id", siteId)
                    parameter("measureKind", "FLOW")
                    parameter("aggregationLevel", "NONE")
                    parameter("start", startTime.toZoneString(timeZone))
                    parameter("end", endTime.toZoneString(timeZone))
                }
            }
        }
    }

    suspend fun fetchDevices(
        siteId: Int,
    ): Either<ApiError, List<DeviceDto>> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("api/devices")
                    parameter("siteId", siteId)
                }
            }
        }
    }

    suspend fun authenticated(): Either<ApiError, UserDto?> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("api/users/authenticated")
                }
            }
        }
    }
}

@Serializable
data class AuthRequest(val username: String, val password: String)