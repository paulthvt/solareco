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
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$serverBaseUrl/api/aggregations/time-series?id=$deviceId&measureKind=FLOW&aggregationLevel=NONE&timeAgoUnit=DAY&timeAgoValue=1&end=2021-09-01T02%3A00%3A00%2B02%3A00"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/time-series-response.json").readText(),
                httpMethod = HttpMethod.Get
            )
        )

        val timeSeries = ComwattApi(client, serverBaseUrl).fetchTimeSeries(deviceId, Instant.parse("2021-09-01T00:00:00Z"))
        assertTrue { timeSeries.isRight() }
        timeSeries.onRight {
            assertTrue { it.values.isNotEmpty() }
            assertTrue { it.timestamps.isNotEmpty() }
            assertEquals(it.values.size, it.timestamps.size)
        }
    }
}