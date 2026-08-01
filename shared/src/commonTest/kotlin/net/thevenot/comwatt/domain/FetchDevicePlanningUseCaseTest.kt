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
import kotlin.test.fail

class FetchDevicePlanningUseCaseTest {

    private val planningsJson =
        Resource("src/commonTest/resources/api/responses/planning-device-get-response.json").readText()
    private val typicalDaysJson =
        Resource("src/commonTest/resources/api/responses/typical-days-get.json").readText()

    /**
     * The device endpoint is deliberately wired to fail: the use case must never
     * call it, since it returns only today's schedules and the planning PUT
     * replaces the whole array.
     */
    private fun engine(
        failSitePlannings: Boolean = false,
        sitePlanningsJson: String = planningsJson,
    ) = MockEngine { request ->
        val path = request.url.encodedPath
        val query = request.url.encodedQuery
        val json = when {
            path == "/api/typicaldays" -> typicalDaysJson
            path == "/api/plannings" && "siteId" in query -> {
                if (failSitePlannings) {
                    return@MockEngine respondError(HttpStatusCode.InternalServerError, "boom")
                }
                sitePlanningsJson
            }
            path == "/api/plannings" && "deviceId" in query ->
                fail("the device-scoped plannings endpoint must not be used: it omits inactive schedules")
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
        val planning = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)
            .getOrNull() ?: fail("expected a right")

        assertNotNull(planning.planningId)
        assertTrue(planning.schedules.isNotEmpty())
    }

    @Test
    fun `marks the generated schedule as server managed`() = runTest {
        val planning = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)
            .getOrNull() ?: fail("expected a right")

        val generated = planning.schedules.filter { it.isServerManaged }
        assertTrue(generated.isNotEmpty(), "fixture has at least one optimalPlanning schedule")
        assertTrue(generated.all { it.typicalDay.label.startsWith("TD-ML-") })
    }

    @Test
    fun `excludes generated days from the picker`() = runTest {
        val planning = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)
            .getOrNull() ?: fail("expected a right")

        assertTrue(planning.availableTypicalDays.isNotEmpty())
        assertFalse(planning.availableTypicalDays.any { it.isServerManaged })
    }

    @Test
    fun `counts how many devices use each typical day`() = runTest {
        val planning = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)
            .getOrNull() ?: fail("expected a right")

        assertTrue(planning.usageCountByTypicalDayId.isNotEmpty())
        assertTrue(planning.usageCountByTypicalDayId.values.all { it >= 1 })
    }

    @Test
    fun `a failed site plannings call is an error, not an empty planning`() = runTest {
        val result = useCase(engine(failSitePlannings = true)).invoke(deviceId = 124758, siteId = 18734)

        assertTrue(
            result.isLeft(),
            "the schedule list comes from this call, so failing it must not yield an empty planning " +
                "that a later write would use to delete every schedule",
        )
    }

    @Test
    fun `keeps the raw planning for write bodies`() = runTest {
        val planning = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)
            .getOrNull() ?: fail("expected a right")

        assertNotNull(planning.rawPlanning)
        assertEquals(planning.planningId, planning.rawPlanning?.id)
    }

    @Test
    fun `a site with no planning for the device yields an empty planning`() = runTest {
        val otherDeviceOnly = planningsJson.replace("\"id\": 124758", "\"id\": 999999")
        val planning = useCase(engine(sitePlanningsJson = otherDeviceOnly))
            .invoke(deviceId = 124758, siteId = 18734)
            .getOrNull() ?: fail("expected a right")

        assertEquals(null, planning.planningId)
        assertEquals(null, planning.rawPlanning)
        assertTrue(planning.schedules.isEmpty())
        assertTrue(
            planning.availableTypicalDays.isNotEmpty(),
            "the picker still works so the user can create the first schedule",
        )
    }

    @Test
    fun `malformed json degrades to a left instead of throwing`() = runTest {
        val engine = MockEngine { request ->
            respond(
                content = if ("siteId" in request.url.encodedQuery) "{not json" else typicalDaysJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        assertTrue(useCase(engine).invoke(deviceId = 124758, siteId = 18734).isLeft())
    }

    /**
     * The regression test for the data-loss bug: a schedule whose date window is
     * in the past is absent from `GET /api/plannings?deviceId=`, so reading from
     * there and writing the result back deleted it. Reading from the site
     * endpoint must carry it through the read → rebuild → write round trip.
     */
    @Test
    fun `a schedule whose window does not cover today survives a read rebuild write round trip`() =
        runTest {
            val expiredSchedule = """
                {
                  "id": 244700,
                  "activeDayMask": 127,
                  "startDate": "2025-01-01",
                  "endDate": "2025-12-31",
                  "optimalPlanning": false,
                  "typicalDay": {
                    "id": 1429676,
                    "label": "Last year",
                    "optimalPlanning": false,
                    "isDefault": false,
                    "timeRangeConfigurations": [
                      { "id": 1, "startTime": "08:00:00", "endTime": "20:00:00", "mode": "ON" }
                    ]
                  }
                },
            """.trimIndent()
            val withExpired = planningsJson.replace(
                "\"typicalDaySchedules\": [",
                "\"typicalDaySchedules\": [$expiredSchedule",
            )

            val planning = useCase(engine(sitePlanningsJson = withExpired))
                .invoke(deviceId = 124758, siteId = 18734)
                .getOrNull() ?: fail("expected a right")

            assertTrue(
                planning.schedules.any { it.typicalDay.label == "Last year" },
                "the read must include schedules outside today's window",
            )

            val raw = planning.rawPlanning ?: fail("expected a raw planning")
            val body = PlanningRebuilder.buildWriteBody(
                current = raw,
                userSchedules = planning.schedules.filterNot { it.isServerManaged },
            )

            assertTrue(
                body.typicalDaySchedules.any { it.typicalDay.label == "Last year" },
                "the expired schedule must be written back, or the PUT deletes it",
            )
            assertEquals(
                setOf("Last year", "Automatic"),
                body.typicalDaySchedules.map { it.typicalDay.label }.toSet(),
            )
        }
}
