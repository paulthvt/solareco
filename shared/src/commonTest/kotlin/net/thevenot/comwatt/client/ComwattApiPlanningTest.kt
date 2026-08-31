package net.thevenot.comwatt.client

import com.goncalossilva.resources.Resource
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import kotlinx.coroutines.test.runTest
import net.thevenot.comwatt.model.PlanningDeviceRefDto
import net.thevenot.comwatt.model.PlanningDto
import net.thevenot.comwatt.model.TimeRangeConfigurationDto
import net.thevenot.comwatt.model.TypicalDayDto
import net.thevenot.comwatt.utils.configureMockEngine
import net.thevenot.comwatt.utils.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComwattApiPlanningTest {

    private val baseUrl = "http://localhost"

    private fun typicalDay() = TypicalDayDto(
        label = "Evening",
        timeRangeConfigurations = listOf(
            TimeRangeConfigurationDto(startTime = "18:00:00", endTime = "22:00:00", mode = "ON"),
        ),
    )

    /** Captures the outgoing body so the test can assert what was serialized. */
    private fun capturingEngine(
        responseBody: String,
        expectedUrl: Url,
        expectedMethod: HttpMethod,
        captured: MutableList<String>,
    ) = MockEngine { request ->
        assertEquals(expectedUrl, request.url)
        assertEquals(expectedMethod, request.method)
        captured += request.body.toByteReadPacketString()
        respond(
            content = responseBody,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    @Test
    fun `fetch typical days parses the paged response`() = runTest {
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$baseUrl/api/typicaldays?siteId=18734"),
                expectedResponseBody = Resource("api/responses/typical-days-get.json").readText(),
                httpMethod = HttpMethod.Get,
            )
        )

        val result = ComwattApi(client, baseUrl).fetchTypicalDays(18734)

        assertTrue(result.isRight())
        result.onRight { assertTrue(it.content.isNotEmpty()) }
    }

    @Test
    fun `create typical day sends siteId as a query parameter`() = runTest {
        val bodies = mutableListOf<String>()
        val client = mockHttpClient(
            capturingEngine(
                responseBody = """{"id":1,"label":"Evening","timeRangeConfigurations":[]}""",
                expectedUrl = Url("$baseUrl/api/typicaldays?siteId=18734"),
                expectedMethod = HttpMethod.Post,
                captured = bodies,
            )
        )

        val result = ComwattApi(client, baseUrl).createTypicalDay(18734, typicalDay())

        assertTrue(result.isRight())
        assertTrue(
            "siteId" !in bodies.single(),
            "siteId in the body returns 400; it must only be a query parameter",
        )
    }

    @Test
    fun `update typical day puts to the day id`() = runTest {
        val bodies = mutableListOf<String>()
        val client = mockHttpClient(
            capturingEngine(
                responseBody = """{"id":1451230,"label":"Evening","timeRangeConfigurations":[]}""",
                expectedUrl = Url("$baseUrl/api/typicaldays/1451230"),
                expectedMethod = HttpMethod.Put,
                captured = bodies,
            )
        )

        val result = ComwattApi(client, baseUrl).updateTypicalDay(1451230, typicalDay())

        assertTrue(result.isRight())
        assertTrue("Evening" in bodies.single())
    }

    @Test
    fun `fetch plannings for a device`() = runTest {
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$baseUrl/api/plannings?deviceId=124758"),
                expectedResponseBody = Resource("api/responses/planning-device-get-response.json").readText(),
                httpMethod = HttpMethod.Get,
            )
        )

        val result = ComwattApi(client, baseUrl).fetchPlannings(124758)

        assertTrue(result.isRight())
        result.onRight { assertTrue(it.content.isNotEmpty()) }
    }

    @Test
    fun `fetch plannings for a site uses siteId`() = runTest {
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$baseUrl/api/plannings?siteId=18734"),
                expectedResponseBody = Resource("api/responses/planning-device-get-response.json").readText(),
                httpMethod = HttpMethod.Get,
            )
        )

        val result = ComwattApi(client, baseUrl).fetchSitePlannings(18734)

        assertTrue(result.isRight())
    }

    @Test
    fun `update planning serializes the device class discriminator`() = runTest {
        val bodies = mutableListOf<String>()
        val client = mockHttpClient(
            capturingEngine(
                responseBody = """{"id":115292,"device":{"id":124758},"typicalDaySchedules":[]}""",
                expectedUrl = Url("$baseUrl/api/plannings/115292"),
                expectedMethod = HttpMethod.Put,
                captured = bodies,
            )
        )

        val result = ComwattApi(client, baseUrl).updatePlanning(
            id = 115292,
            body = PlanningDto(id = 115292, device = PlanningDeviceRefDto(id = 124758)),
        )

        assertTrue(result.isRight())
        assertTrue(
            """"@class":"Device"""" in bodies.single(),
            "without @class the API returns 400 Failed to read request",
        )
    }

    @Test
    fun `set capacity switch passes enable as a query parameter`() = runTest {
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$baseUrl/api/capacities/318273/switch?enable=false"),
                expectedResponseBody = """{"id":318273,"enable":false}""",
                httpMethod = HttpMethod.Put,
            )
        )

        val result = ComwattApi(client, baseUrl).setCapacitySwitch(318273, enable = false)

        assertTrue(result.isRight())
    }
}

/** Reads a Ktor outgoing body back as a string, for asserting on serialized JSON. */
private suspend fun Any.toByteReadPacketString(): String = when (this) {
    is io.ktor.http.content.TextContent -> text
    is io.ktor.http.content.OutgoingContent.ByteArrayContent -> bytes().decodeToString()
    else -> toString()
}
