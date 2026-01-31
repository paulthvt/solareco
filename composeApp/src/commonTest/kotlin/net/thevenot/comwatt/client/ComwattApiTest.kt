package net.thevenot.comwatt.client

import com.goncalossilva.resources.Resource
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.utils.configureMockEngine
import net.thevenot.comwatt.utils.mockHttpClient
import net.thevenot.comwatt.utils.toZoneString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

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

        // Use the system timezone to generate the expected URL, matching what the API client will do
        val timeZone = TimeZone.currentSystemDefault()
        val expectedEndTimeString = endTime.toZoneString(timeZone)

        // URL-encode the timestamp to match what Ktor actually sends
        val encodedEndTime = expectedEndTimeString.replace(":", "%3A").replace("+", "%2B")
        
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$serverBaseUrl/api/aggregations/time-series?id=$deviceId&measureKind=FLOW&aggregationLevel=NONE&timeAgoUnit=DAY&timeAgoValue=1&end=$encodedEndTime"),
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

    @Test
    fun `fetch electricity price`() = runTest {
        val serverBaseUrl = "http://localhost"
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$serverBaseUrl/api/electricityprice"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/eletricity-price.json").readText(),
                httpMethod = HttpMethod.Get
            )
        )

        val electricityPrice = ComwattApi(client, serverBaseUrl).fetchElectricityPrice()
        assertTrue { electricityPrice.isRight() }
        electricityPrice.onRight { response ->
            // Verify tempo syntheses
            assertTrue { response.tempoSynthesesComplete }
            assertEquals(119, response.tempoSyntheses.blue.numberOfDays)
            assertEquals(300, response.tempoSyntheses.blue.totalNumberOfDays)
            assertEquals(26, response.tempoSyntheses.white.numberOfDays)
            assertEquals(43, response.tempoSyntheses.white.totalNumberOfDays)
            assertEquals(8, response.tempoSyntheses.red.numberOfDays)
            assertEquals(22, response.tempoSyntheses.red.totalNumberOfDays)

            // Verify daily data
            assertEquals(2, response.daily.size)

            // First day
            val firstDay = response.daily[0]
            assertEquals("2026-01-30", firstDay.date)
            assertEquals(net.thevenot.comwatt.model.TempoDayValue.BLUE, firstDay.dayValue)
            assertEquals(3, firstDay.status.size)

            // Second day
            val secondDay = response.daily[1]
            assertEquals("2026-01-31", secondDay.date)
            assertEquals(net.thevenot.comwatt.model.TempoDayValue.WHITE, secondDay.dayValue)
        }
    }
}