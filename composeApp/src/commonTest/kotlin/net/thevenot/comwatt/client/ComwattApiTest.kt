package net.thevenot.comwatt.client

import com.goncalossilva.resources.Resource
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import net.thevenot.comwatt.utils.configureMockEngine
import net.thevenot.comwatt.utils.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComwattApiTest {
    @Test
    fun `fetch sites`() = runTest {
        val serverBaseUrl = "http://localhost"
        val siteId = 132
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$serverBaseUrl/api/devices?siteId=$siteId"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/devices-response.json").readText(),
                httpMethod = HttpMethod.Get
            )
        )

        val fetchDevices = ComwattApi(client, serverBaseUrl).fetchDevices(siteId)
        assertTrue { fetchDevices.isRight() }
        fetchDevices.onRight {
            assertEquals(11, it.size)
        }
    }

    @Test
    fun `fetch time series`() = runTest {
        val serverBaseUrl = "http://localhost"
        val deviceId = 132
        val endTime = Instant.parse("2021-09-01T00:00:00Z")
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$serverBaseUrl/api/aggregations/time-series?id=$deviceId&measureKind=FLOW&aggregationLevel=NONE&timeAgoUnit=DAY&timeAgoValue=1&end=2021-09-01T02%3A00%3A00%2B02%3A00"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/time-series-response.json").readText(),
                httpMethod = HttpMethod.Get
            )
        )

        val timeSeries = ComwattApi(client, serverBaseUrl).fetchTimeSeries(
            deviceId = deviceId,
            endTime = endTime,
            timeAgoUnit = net.thevenot.comwatt.model.type.TimeAgoUnit.DAY,
            timeAgoValue = 1
        )
        assertTrue { timeSeries.isRight() }
        timeSeries.onRight {
            assertTrue { it.values.isNotEmpty() }
            assertTrue { it.timestamps.isNotEmpty() }
            assertEquals(it.values.size, it.timestamps.size)
        }
    }

    @Test
    fun `fetch tiles`() = runTest {
        val serverBaseUrl = "http://localhost"
        val siteId = 132
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$serverBaseUrl/api/tiles?siteId=$siteId&tileTypes=THIRD_PARTY&tileTypes=VALUATION"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/tiles-response.json").readText(),
                httpMethod = HttpMethod.Get
            )
        )

        val fetchTiles = ComwattApi(client, serverBaseUrl).fetchTiles(siteId)
        assertTrue { fetchTiles.isRight() }
        fetchTiles.onRight {
            assertEquals(11, it.size)
        }
    }

    @Test
    fun `fetch site`() = runTest {
        val serverBaseUrl = "http://localhost"
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$serverBaseUrl/api/sites"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/sites-response.json").readText(),
                httpMethod = HttpMethod.Get
            )
        )

        val sites = ComwattApi(client, serverBaseUrl).sites()
        assertTrue { sites.isRight() }
        sites.onRight {
            assertEquals(1, it.size)
            assertEquals(18734, it[0].id)
        }
    }

    @Test
    fun `fetch user`() = runTest {
        val serverBaseUrl = "http://localhost"
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$serverBaseUrl/api/users/authenticated"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/user-response.json").readText(),
                httpMethod = HttpMethod.Get
            )
        )

        val user = ComwattApi(client, serverBaseUrl).authenticated()
        assertTrue { user.isRight() }
        user.onRight {
            assertEquals(23481, it?.id)
        }
    }

    @Test
    fun `fetch daily weather forecast`() = runTest {
        val serverBaseUrl = "http://localhost"
        val zip = "83560"
        val countryCode = "FR"
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$serverBaseUrl/weather/data/2.5/forecast/daily?zip=83560%2CFR&units=metric&lang=en"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/daily-response.json").readText(),
                httpMethod = HttpMethod.Get
            )
        )

        val weatherForecast =
            ComwattApi(client, serverBaseUrl).fetchDailyWeatherForecast(zip, countryCode)
        assertTrue { weatherForecast.isRight() }
        weatherForecast.onRight { response ->
            assertEquals("200", response.cod)
            assertEquals(7, response.cnt)
            assertEquals("Aix-en-Provence", response.city.name)
            assertEquals("FR", response.city.country)
            assertEquals(5.4467, response.city.coord.lon)
            assertEquals(43.5283, response.city.coord.lat)
            assertEquals(7, response.list.size)

            // Verify first weather entry
            val firstWeather = response.list.first()
            assertEquals(1753009200L, firstWeather.dt)
            assertEquals(25.33, firstWeather.temp.day)
            assertEquals(19.74, firstWeather.temp.min)
            assertEquals(27.56, firstWeather.temp.max)
            assertEquals(1008, firstWeather.pressure)
            assertEquals(63, firstWeather.humidity)
            assertEquals(1, firstWeather.weather.size)
            assertEquals("Rain", firstWeather.weather.first().main)
            assertEquals("light rain", firstWeather.weather.first().description)
            assertEquals(1.64, firstWeather.rain)
        }
    }
}