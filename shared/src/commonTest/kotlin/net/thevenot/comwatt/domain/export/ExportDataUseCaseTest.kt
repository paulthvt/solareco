package net.thevenot.comwatt.domain.export

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.utils.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExportDataUseCaseTest {
    private val start = Instant.parse("2026-01-12T00:00:00Z")
    private val end = Instant.parse("2026-01-12T02:00:00Z")

    private val devicesBody = """
        [
          {"id":1,"name":"four","deviceKind":{"code":"OVEN","global":false}},
          {"id":2,"name":"échange réseau","deviceKind":{"code":"GRID_METER","global":true}},
          {"name":"ghost"}
        ]
    """.trimIndent()

    private val siteBody = """
        {
          "timestamps":["2026-01-12T00:00:00Z","2026-01-12T01:00:00Z"],
          "productions":[0.0,0.0],
          "consumptions":[100.0,200.0],
          "injections":[0.0,0.0],
          "withdrawals":[100.0,200.0],
          "charges":[],
          "discharges":[],
          "autoproductionRates":[],
          "autoconsumptionRates":[],
          "injectionRates":[],
          "withdrawalRates":[]
        }
    """.trimIndent()

    private fun deviceBody(value: Double) = """
        {"timestamps":["2026-01-12T00:00:00Z","2026-01-12T01:00:00Z"],"values":[$value,$value]}
    """.trimIndent()

    private val emptySiteBody = """
        {
          "timestamps":[],"productions":[],"consumptions":[],"injections":[],"withdrawals":[],
          "charges":[],"discharges":[],"autoproductionRates":[],"autoconsumptionRates":[],
          "injectionRates":[],"withdrawalRates":[]
        }
    """.trimIndent()

    private val emptyDeviceBody = """{"timestamps":[],"values":[]}"""

    /** Records every request URL so the tests can assert on the outgoing query. */
    private val requestedUrls = mutableListOf<String>()

    private fun engine(
        devices: String = devicesBody,
        site: String = siteBody,
        device: (index: Int) -> String = { deviceBody(10.0) },
        failFirstDeviceWith: HttpStatusCode? = null
    ): MockEngine {
        var deviceCalls = 0
        return MockEngine { request ->
            val url = request.url.toString()
            requestedUrls += url
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            when {
                url.contains("/api/devices") -> respond(devices, HttpStatusCode.OK, json)
                url.contains("site-time-series") -> respond(site, HttpStatusCode.OK, json)
                else -> {
                    val index = deviceCalls++
                    if (index == 0 && failFirstDeviceWith != null) {
                        respondError(failFirstDeviceWith)
                    } else {
                        respond(device(index), HttpStatusCode.OK, json)
                    }
                }
            }
        }
    }

    // ComwattApi requires (HttpClient, baseUrl: String); "" suffices for MockEngine routing.
    private fun useCase(
        engine: MockEngine,
        siteId: Int? = 18734,
        onUnauthorized: suspend () -> Unit = {}
    ) = ExportDataUseCase(
        api = ComwattApi(mockHttpClient(engine), ""),
        siteIdProvider = { siteId },
        onUnauthorized = onUnauthorized
    )

    @Test
    fun requestsUseHourlyQuantityAndNeverAggregationType() = runTest {
        requestedUrls.clear()

        useCase(engine()).execute(start, end, TimeZone.UTC) { _, _ -> }

        val seriesUrls = requestedUrls.filter { it.contains("time-series") }
        assertTrue(seriesUrls.isNotEmpty())
        seriesUrls.forEach { url ->
            assertTrue(url.contains("aggregationLevel=HOUR"), "missing aggregationLevel in $url")
            assertTrue(url.contains("measureKind=QUANTITY"), "missing measureKind in $url")
            assertTrue(!url.contains("aggregationType"), "aggregationType present in $url")
        }
    }

    @Test
    fun devicesWithoutAnIdAreSkippedEntirely() = runTest {
        var total = 0

        useCase(engine()).execute(start, end, TimeZone.UTC) { _, t -> total = t }

        // 1 site + 2 devices with ids; "ghost" has no id.
        assertEquals(3, total)
    }

    @Test
    fun progressReachesTheTotal() = runTest {
        var highest = 0
        var total = 0

        // Callbacks may arrive out of order under concurrency; only the peak is meaningful.
        useCase(engine()).execute(start, end, TimeZone.UTC) { c, t ->
            highest = maxOf(highest, c)
            total = t
        }

        assertEquals(total, highest)
    }

    @Test
    fun csvCarriesSiteTotalsAndDeviceColumns() = runTest {
        val outcome = useCase(engine()).execute(start, end, TimeZone.UTC) { _, _ -> }
            .getOrNull()

        val csv = (outcome as ExportOutcome.Csv).content
        val header = csv.lines().first { !it.startsWith("#") }
        assertEquals(
            "timestamp,production_wh,consumption_wh,injection_wh,withdrawal_wh,four,échange réseau",
            header
        )
        assertTrue(csv.contains("Summing all device columns"))
    }

    @Test
    fun fileNameCarriesTheRangeAndGranularity() = runTest {
        val outcome = useCase(engine()).execute(start, end, TimeZone.UTC) { _, _ -> }
            .getOrNull()

        assertEquals(
            "solareco-2026-01-12_2026-01-12-hourly.csv",
            (outcome as ExportOutcome.Csv).fileName
        )
    }

    @Test
    fun oneFailingSeriesFailsTheWholeExport() = runTest {
        val result = useCase(engine(failFirstDeviceWith = HttpStatusCode.InternalServerError))
            .execute(start, end, TimeZone.UTC) { _, _ -> }

        assertTrue(result.isLeft())
    }

    @Test
    fun unauthorizedTriggersExactlyOneAutoLoginRetry() = runTest {
        var autoLogins = 0

        val result = useCase(
            engine = engine(failFirstDeviceWith = HttpStatusCode.Unauthorized),
            onUnauthorized = { autoLogins++ }
        ).execute(start, end, TimeZone.UTC) { _, _ -> }

        assertEquals(1, autoLogins)
        assertTrue(result.isRight())
    }

    @Test
    fun allEmptySeriesGiveNoDataAndNoFile() = runTest {
        val result = useCase(
            engine(site = emptySiteBody, device = { emptyDeviceBody })
        ).execute(start, end, TimeZone.UTC) { _, _ -> }

        assertEquals(ExportOutcome.NoData, result.getOrNull())
    }

    @Test
    fun missingSiteIdFails() = runTest {
        val result = useCase(engine(), siteId = null)
            .execute(start, end, TimeZone.UTC) { _, _ -> }

        assertTrue(result.isLeft())
    }
}
