package net.thevenot.comwatt.domain

import com.goncalossilva.resources.Resource
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.utils.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FetchDevicePlanningUseCaseTest {

    private val planningsJson =
        Resource("src/commonTest/resources/api/responses/planning-device-get-response.json").readText()
    private val typicalDaysJson =
        Resource("src/commonTest/resources/api/responses/typical-days-get.json").readText()

    private fun engine(failSitePlannings: Boolean = false) = MockEngine { request ->
        val path = request.url.encodedPath
        val query = request.url.encodedQuery
        val json = when {
            path == "/api/typicaldays" -> typicalDaysJson
            path == "/api/plannings" && "siteId" in query -> {
                if (failSitePlannings) {
                    return@MockEngine respondError(HttpStatusCode.InternalServerError, "boom")
                }
                planningsJson
            }
            path == "/api/plannings" -> planningsJson
            else -> error("unexpected request: $path?$query")
        }
        respond(
            content = json,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    private fun useCase(engine: MockEngine) =
        FetchDevicePlanningUseCase(ComwattApi(mockHttpClient(engine), "http://localhost"))

    @Test
    fun `loads the device's schedules`() = runTest {
        val result = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)

        assertTrue(result.isRight())
        result.onRight { planning ->
            assertNotNull(planning.planningId)
            assertTrue(planning.schedules.isNotEmpty())
        }
    }

    @Test
    fun `marks the generated schedule as server managed`() = runTest {
        val result = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)

        result.onRight { planning ->
            val generated = planning.schedules.filter { it.isServerManaged }
            assertTrue(generated.isNotEmpty(), "fixture has at least one optimalPlanning schedule")
            assertTrue(generated.all { it.typicalDay.label.startsWith("TD-ML-") })
        }
    }

    @Test
    fun `excludes generated days from the picker`() = runTest {
        val result = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)

        result.onRight { planning ->
            assertTrue(planning.availableTypicalDays.isNotEmpty())
            assertFalse(planning.availableTypicalDays.any { it.isServerManaged })
        }
    }

    @Test
    fun `counts how many devices use each typical day`() = runTest {
        val result = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)

        result.onRight { planning ->
            assertTrue(planning.usageCountByTypicalDayId.isNotEmpty())
            assertTrue(planning.usageCountByTypicalDayId.values.all { it >= 1 })
        }
    }

    @Test
    fun `a failed site plannings call still loads the tab without counts`() = runTest {
        val result = useCase(engine(failSitePlannings = true)).invoke(deviceId = 124758, siteId = 18734)

        assertTrue(result.isRight())
        result.onRight { planning ->
            assertTrue(planning.schedules.isNotEmpty())
            assertEquals(emptyMap(), planning.usageCountByTypicalDayId)
        }
    }

    @Test
    fun `keeps the raw planning for write bodies`() = runTest {
        val result = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)

        result.onRight { planning ->
            assertNotNull(planning.rawPlanning)
            assertEquals(planning.planningId, planning.rawPlanning?.id)
        }
    }
}
