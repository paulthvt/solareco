package net.thevenot.comwatt.domain.export

import arrow.core.Either
import arrow.core.raise.either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.DeviceDto
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.TimeSeriesDto
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.MeasureKind
import net.thevenot.comwatt.utils.toZoneString
import kotlin.time.Instant

internal sealed interface ExportOutcome {
    data class Csv(val fileName: String, val content: String) : ExportOutcome
    data object NoData : ExportOutcome
}

/**
 * Fetches 1 site series + N device series at hourly resolution and renders them as one CSV.
 *
 * Takes lambdas rather than `DataRepository` so it can be tested against `MockEngine` alone.
 */
internal class ExportDataUseCase(
    private val api: ComwattApi,
    private val siteIdProvider: suspend () -> Int?,
    private val onUnauthorized: suspend () -> Unit
) {
    suspend fun execute(
        startTime: Instant,
        endTime: Instant,
        timeZone: TimeZone,
        onProgress: suspend (completed: Int, total: Int) -> Unit
    ): Either<DomainError, ExportOutcome> = either {
        val siteId = siteIdProvider() ?: raise(DomainError.Generic("Site id not found"))

        val devices = api.fetchDevices(siteId)
            .mapLeft { DomainError.Api(it) }
            .bind()
            .filter { it.id != null }

        val total = devices.size + 1
        var completed = 0
        val progressLock = Mutex()
        suspend fun reportOne() {
            val done = progressLock.withLock { ++completed }
            onProgress(done, total)
        }

        val semaphore = Semaphore(CONCURRENT_REQUESTS)
        // One login per export, shared by however many series 401 at once.
        val loginOnce = LoginOnce(onUnauthorized)
        val (site, deviceSeries) = coroutineScope {
            val siteDeferred = async {
                semaphore.withPermit {
                    withUnauthorizedRetry(loginOnce) { fetchSite(siteId, startTime, endTime) }
                        .also { reportOne() }
                }
            }
            val deviceDeferreds = devices.map { device ->
                async {
                    semaphore.withPermit {
                        withUnauthorizedRetry(loginOnce) { fetchDevice(device.id!!, startTime, endTime) }
                            .map { device.toExportColumn() to it }
                            .also { reportOne() }
                    }
                }
            }
            siteDeferred.await() to deviceDeferreds.awaitAll()
        }

        val siteDto = site.mapLeft { DomainError.Api(it) }.bind()
        val columns = deviceSeries.map { it.mapLeft { error -> DomainError.Api(error) }.bind() }

        val table = buildExportTable(site = siteDto, devices = columns)
        if (table.timestamps.isEmpty()) {
            Logger.d(TAG) { "export produced no rows for site $siteId" }
            return@either ExportOutcome.NoData
        }

        val metadata = ExportMetadata(siteId = siteId, startTime = startTime, endTime = endTime)
        ExportOutcome.Csv(
            fileName = fileNameFor(startTime, endTime, timeZone),
            content = CsvWriter.write(table, metadata, timeZone).joinToString("\n")
        )
    }

    /**
     * The export has no retry loop of its own: a single 401 means the session expired mid-export,
     * so re-login once and re-issue that one series. Anything else fails the export.
     *
     * Concurrent 401s all wait on [loginOnce], so the session is renewed once per export rather
     * than once per in-flight series.
     */
    private suspend fun <T> withUnauthorizedRetry(
        loginOnce: LoginOnce,
        block: suspend () -> Either<ApiError, T>
    ): Either<ApiError, T> {
        val first = block()
        val error = first.leftOrNull() ?: return first
        if (error !is ApiError.HttpError || error.code != 401) return first
        Logger.d(TAG) { "401 during export, re-authenticating once" }
        loginOnce.await()
        return block()
    }

    private suspend fun fetchSite(
        siteId: Int,
        startTime: Instant,
        endTime: Instant
    ): Either<ApiError, SiteTimeSeriesDto> = api.fetchSiteTimeSeries(
        siteId = siteId,
        startTime = startTime,
        endTime = endTime,
        measureKind = MeasureKind.QUANTITY,
        aggregationLevel = AggregationLevel.HOUR
    )

    private suspend fun fetchDevice(
        deviceId: Int,
        startTime: Instant,
        endTime: Instant
    ): Either<ApiError, TimeSeriesDto> = api.fetchTimeSeries(
        deviceId = deviceId,
        startTime = startTime,
        endTime = endTime,
        measureKind = MeasureKind.QUANTITY,
        aggregationLevel = AggregationLevel.HOUR
    )

    private fun fileNameFor(startTime: Instant, endTime: Instant, timeZone: TimeZone): String {
        val from = startTime.toZoneString(timeZone).substringBefore('T')
        val to = endTime.toZoneString(timeZone).substringBefore('T')
        return "solareco-${from}_$to-hourly.csv"
    }

    companion object {
        private const val TAG = "ExportDataUseCase"

        /** Three at a time keeps a year-long export around five seconds without hammering the API. */
        private const val CONCURRENT_REQUESTS = 3
    }
}

/**
 * Runs [login] at most once, however many callers reach it. Late callers suspend until the first
 * one's attempt has finished, so their retry carries the renewed session cookie.
 */
private class LoginOnce(private val login: suspend () -> Unit) {
    private val mutex = Mutex()
    private var done = false

    suspend fun await() {
        mutex.withLock {
            if (done) return
            done = true
            login()
        }
    }
}

/**
 * Column metadata comes from `deviceKind`, never from the device name. Names are user-editable and
 * site-specific, so matching on them would break on rename and on anyone else's site.
 */
internal fun DeviceDto.toExportColumn(): ExportColumn = ExportColumn(
    name = name ?: "device $id",
    deviceCode = deviceKind?.code,
    isSiteLevelMeter = deviceKind?.global == true
)
