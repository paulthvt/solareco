package net.thevenot.comwatt.client

import arrow.core.Either
import arrow.core.flatMap
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.addCookie
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.DailyWeatherResponseDto
import net.thevenot.comwatt.model.DeviceDto
import net.thevenot.comwatt.model.SiteDto
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.TileResponseDto
import net.thevenot.comwatt.model.TileType
import net.thevenot.comwatt.model.TimeSeriesDto
import net.thevenot.comwatt.model.UserDto
import net.thevenot.comwatt.model.safeRequest
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.AggregationType
import net.thevenot.comwatt.model.type.MeasureKind
import net.thevenot.comwatt.model.type.TimeAgoUnit
import net.thevenot.comwatt.utils.toZoneString
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

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
            Logger.d { "Session token $sessionToken" }
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

    private suspend fun doFetchSiteTimeSeries(
        siteId: Int,
        startTime: Instant? = null,
        endTime: Instant = Clock.System.now(),
        timeAgoUnit: TimeAgoUnit? = null,
        timeAgoValue: Int? = null,
        measureKind: MeasureKind = MeasureKind.FLOW,
        aggregationLevel: AggregationLevel = AggregationLevel.NONE,
        aggregationType: AggregationType? = null
    ): Either<ApiError, SiteTimeSeriesDto> {
        val timeZone = TimeZone.currentSystemDefault()

        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("api/aggregations/site-time-series")
                    parameter("id", siteId)
                    parameter("measureKind", measureKind)
                    parameter("aggregationLevel", aggregationLevel)
                    startTime?.let { parameter("start", it.toZoneString(timeZone)) }
                    timeAgoUnit?.let { parameter("timeAgoUnit", it) }
                    timeAgoValue?.let { parameter("timeAgoValue", it) }
                    aggregationType?.let { parameter("aggregationType", it) }
                    parameter("end", endTime.toZoneString(timeZone))
                }
            }
        }
    }

    suspend fun fetchSiteTimeSeries(
        siteId: Int,
        startTime: Instant = Clock.System.now().minus(5, DateTimeUnit.MINUTE),
        endTime: Instant = Clock.System.now(),
        measureKind: MeasureKind = MeasureKind.FLOW,
        aggregationLevel: AggregationLevel = AggregationLevel.NONE,
        aggregationType: AggregationType? = null
    ): Either<ApiError, SiteTimeSeriesDto> {
        return doFetchSiteTimeSeries(
            siteId = siteId,
            startTime = startTime,
            endTime = endTime,
            measureKind = measureKind,
            aggregationLevel = aggregationLevel,
            aggregationType = aggregationType
        )
    }

    suspend fun fetchSiteTimeSeries(
        siteId: Int,
        timeAgoUnit: TimeAgoUnit,
        timeAgoValue: Int = 1,
        endTime: Instant = Clock.System.now(),
        measureKind: MeasureKind = MeasureKind.FLOW,
        aggregationLevel: AggregationLevel = AggregationLevel.NONE,
        aggregationType: AggregationType? = null
    ): Either<ApiError, SiteTimeSeriesDto> {
        return doFetchSiteTimeSeries(
            siteId = siteId,
            timeAgoUnit = timeAgoUnit,
            timeAgoValue = timeAgoValue,
            endTime = endTime,
            measureKind = measureKind,
            aggregationLevel = aggregationLevel,
            aggregationType = aggregationType
        )
    }

    suspend fun fetchTiles(
        siteId: Int,
        tileTypes: List<TileType> = TileType.entries,
    ): Either<ApiError, List<TileResponseDto>> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("api/tiles")
                    parameter("siteId", siteId)
                    tileTypes.forEach {
                        parameter("tileTypes", it)
                    }
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

    private suspend fun doFetchTimeSeries(
        deviceId: Int,
        startTime: Instant? = null,
        endTime: Instant = Clock.System.now(),
        timeAgoUnit: TimeAgoUnit? = null,
        timeAgoValue: Int? = null,
        measureKind: MeasureKind = MeasureKind.FLOW,
        aggregationLevel: AggregationLevel = AggregationLevel.NONE,
        aggregationType: AggregationType? = null
    ): Either<ApiError, TimeSeriesDto> {
        val timeZone = TimeZone.currentSystemDefault()

        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("api/aggregations/time-series")
                    parameter("id", deviceId)
                    parameter("measureKind", measureKind)
                    parameter("aggregationLevel", aggregationLevel)
                    startTime?.let { parameter("start", it.toZoneString(timeZone)) }
                    timeAgoUnit?.let { parameter("timeAgoUnit", it) }
                    timeAgoValue?.let { parameter("timeAgoValue", it) }
                    aggregationType?.let { parameter("aggregationType", it) }
                    parameter("end", endTime.toZoneString(timeZone))
                }
            }
        }
    }

    suspend fun fetchTimeSeries(
        deviceId: Int,
        startTime: Instant = Clock.System.now().minus(5, DateTimeUnit.MINUTE),
        endTime: Instant = Clock.System.now(),
        measureKind: MeasureKind = MeasureKind.FLOW,
        aggregationLevel: AggregationLevel = AggregationLevel.NONE,
        aggregationType: AggregationType? = null
    ): Either<ApiError, TimeSeriesDto> {
        return doFetchTimeSeries(
            deviceId = deviceId,
            startTime = startTime,
            endTime = endTime,
            measureKind = measureKind,
            aggregationLevel = aggregationLevel,
            aggregationType = aggregationType
        )
    }

    suspend fun fetchTimeSeries(
        deviceId: Int,
        endTime: Instant = Clock.System.now(),
        timeAgoUnit: TimeAgoUnit = TimeAgoUnit.DAY,
        timeAgoValue: Int = 1,
        measureKind: MeasureKind = MeasureKind.FLOW,
        aggregationLevel: AggregationLevel = AggregationLevel.NONE,
        aggregationType: AggregationType? = null
    ): Either<ApiError, TimeSeriesDto> {
        return doFetchTimeSeries(
            deviceId = deviceId,
            endTime = endTime,
            timeAgoUnit = timeAgoUnit,
            timeAgoValue = timeAgoValue,
            measureKind = measureKind,
            aggregationLevel = aggregationLevel,
            aggregationType = aggregationType
        )
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

    suspend fun fetchDailyWeatherForecast(
        zip: String,
        countryCode: String = "FR",
        units: String = "metric",
        lang: String = "en"
    ): Either<ApiError, DailyWeatherResponseDto> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("weather/data/2.5/forecast/daily")
                    parameter("zip", "$zip,$countryCode")
                    parameter("units", units)
                    parameter("lang", lang)
                }
            }
        }
    }
}

@Serializable
data class AuthRequest(val username: String, val password: String)