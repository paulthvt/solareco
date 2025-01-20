package net.thevenot.comwatt.client

import com.goncalossilva.resources.Resource
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
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
}