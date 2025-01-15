package net.thevenot.comwatt.domain

import arrow.core.Either
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.plus
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.ApiException
import net.thevenot.comwatt.domain.exception.ApiGenericException
import net.thevenot.comwatt.domain.exception.UnauthorizedException
import net.thevenot.comwatt.domain.model.SiteTimeSeries
import net.thevenot.comwatt.model.ApiError

class FetchSiteTimeSeriesUseCase(private val dataRepository: DataRepository) {
    operator fun invoke(): Flow<SiteTimeSeries> = flow {
        while (true) {
            when (val data = refreshSiteData()) {
                is Either.Left -> {
                    if(data.value is UnauthorizedException) {
                        dataRepository.tryAutoLogin({}, {})
                    }
                    else data.value
                }
                is Either.Right -> {
                    val delayMillis = computeDelay(data.value.lastUpdateTimestamp)
                    emit(data.value)
                    Napier.d(tag = TAG) { "waiting for $delayMillis milliseconds" }
                    delay(delayMillis)
                }
            }
        }
    }

    suspend fun singleFetch(): Either<ApiException, SiteTimeSeries> {
        return refreshSiteData()
    }

    private suspend fun refreshSiteData(): Either<ApiException, SiteTimeSeries> {
        Napier.d(tag = TAG) { "calling site time series" }
        val siteId = dataRepository.getSettings().firstOrNull()?.siteId
        siteId?.let { id ->
            when (val response = dataRepository.api.fetchSiteTimeSeries(id)) {
                is Either.Left -> {
                    val error = response.value
                    Napier.d(tag = TAG) { "error: $response.value" }
                    when(error) {
                        is ApiError.HttpError -> {
                            if (error.code == 401) {
                                return Either.Left(UnauthorizedException("Unauthorized"))
                            }
                        }
                        is ApiError.GenericError -> {
                            return Either.Left(ApiGenericException(error.message))
                        }
                        is ApiError.SerializationError -> {
                            return Either.Left(ApiGenericException(error.message))
                        }
                    }
                }

                is Either.Right -> {
                    val lastUpdateTimestamp =
                        Instant.parse(response.value.timestamps.last().toString())

                    return Either.Right(
                        SiteTimeSeries(
                            production = response.value.productions.last(),
                            consumption = response.value.consumptions.last(),
                            injection = response.value.injections.last(),
                            withdrawals = response.value.withdrawals.last(),
                            consumptionRate = response.value.consumptions.last() / MAX_POWER,
                            productionRate = response.value.productions.last() / MAX_POWER,
                            injectionRate = response.value.injections.last() / MAX_POWER,
                            withdrawalsRate = response.value.withdrawals.last() / MAX_POWER,
                            lastUpdateTimestamp = lastUpdateTimestamp,
                            updateDate = lastUpdateTimestamp.format(DateTimeComponents.Formats.RFC_1123),
                            lastRefreshDate = Clock.System.now()
                                .format(DateTimeComponents.Formats.RFC_1123),
                        )
                    )
                }
            }
        }
        return Either.Left(ApiGenericException("Site id not found"))
    }

    private fun computeDelay(lastUpdateTimestamp: Instant): Long {
        val nextUpdateTimestamp = lastUpdateTimestamp.plus(2, DateTimeUnit.MINUTE).plus(5, DateTimeUnit.SECOND)
        val delayMillis = (nextUpdateTimestamp.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0)
        if (delayMillis == 0L) {
            Napier.d(tag = TAG) { "no delay, using fallback delay $FALLBACK_DELAY" }
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