package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.SiteTimeSeries
import net.thevenot.comwatt.model.ApiError

class FetchSiteTimeSeriesUseCase(private val dataRepository: DataRepository) {
    operator fun invoke(): Flow<Either<DomainError, SiteTimeSeries>> = flow {
        while (true) {
            val data = refreshSiteData()
            emit(data)

            when (data) {
                is Either.Left -> {
                    Logger.e(TAG) { "Error fetching site time services: ${data.value}" }
                    val value = data.value
                    if (value is DomainError.Api && value.error is ApiError.HttpError && value.error.code == 401) {
                        dataRepository.tryAutoLogin({}, {})
                    }
                    delay(10_000L)
                }
                is Either.Right -> {
                    val delayMillis = computeDelay(data.value.lastUpdateTimestamp)
                    Logger.d(TAG) { "waiting for $delayMillis milliseconds" }
                    delay(delayMillis)
                }
            }
        }
    }

    suspend fun singleFetch(): Either<DomainError, SiteTimeSeries> {
        return refreshSiteData()
    }

    private suspend fun refreshSiteData(): Either<DomainError, SiteTimeSeries> {
        Logger.d(TAG) { "calling site time series" }
        val siteId = dataRepository.getSettings().firstOrNull()?.siteId
        return siteId?.let { id ->
            dataRepository.api.fetchSiteTimeSeries(siteId = id, startTime = Clock.System.now().minus(5, DateTimeUnit.MINUTE))
                .mapLeft { DomainError.Api(it) }
                .map { timeSeries ->
                    val lastUpdateTimestamp =
                        Instant.parse(timeSeries.timestamps.last())
                    SiteTimeSeries(
                        production = timeSeries.productions.last(),
                        consumption = timeSeries.consumptions.last(),
                        injection = timeSeries.injections.last(),
                        withdrawals = timeSeries.withdrawals.last(),
                        consumptionRate = timeSeries.consumptions.last() / MAX_POWER,
                        productionRate = timeSeries.productions.last() / MAX_POWER,
                        injectionRate = timeSeries.injections.last() / MAX_POWER,
                        withdrawalsRate = timeSeries.withdrawals.last() / MAX_POWER,
                        lastUpdateTimestamp = lastUpdateTimestamp,
                        updateDate = lastUpdateTimestamp.format(DateTimeComponents.Formats.RFC_1123),
                        lastRefreshDate = Clock.System.now()
                            .format(DateTimeComponents.Formats.RFC_1123),
                    )
                }
        } ?: Either.Left(DomainError.Generic("Site id not found"))
    }

    private fun computeDelay(lastUpdateTimestamp: Instant): Long {
        val nextUpdateTimestamp = lastUpdateTimestamp.plus(2, DateTimeUnit.MINUTE).plus(5, DateTimeUnit.SECOND)
        val delayMillis = (nextUpdateTimestamp.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0)
        if (delayMillis == 0L) {
            Logger.d(TAG) { "no delay, using fallback delay $FALLBACK_DELAY" }
            return FALLBACK_DELAY
        }
        return delayMillis
    }

    companion object {
        private const val FALLBACK_DELAY = 10_000L
        const val MAX_POWER = 9000.0
        private const val TAG = "FetchSiteTimeSeriesUseCase"
    }
}