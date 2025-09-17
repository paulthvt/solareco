package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.SiteDailyData
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.AggregationType
import net.thevenot.comwatt.model.type.MeasureKind
import kotlin.time.Clock
import kotlin.time.Instant

class FetchSiteDailyDataUseCase(private val dataRepository: DataRepository) {

    operator fun invoke(): Flow<Either<DomainError, SiteDailyData>> = flow {
        while (true) {
            val data = fetchDailyData()
            emit(data)

            when (data) {
                is Either.Left -> {
                    Logger.e(TAG) { "Error fetching site daily data: ${data.value}" }
                    val value = data.value
                    if (value is DomainError.Api && value.error is ApiError.HttpError && value.error.code == 401) {
                        dataRepository.tryAutoLogin({}, {})
                    }
                    delay(10_000L)
                }

                is Either.Right -> {
                    val delayMillis = 60_000L // 1 minutes for daily data updates
                    Logger.d(TAG) { "Daily data fetched successfully, waiting for $delayMillis milliseconds" }
                    delay(delayMillis)
                }
            }
        }
    }

    suspend fun singleFetch(): Either<DomainError, SiteDailyData> {
        return fetchDailyData()
    }

    private suspend fun fetchDailyData(): Either<DomainError, SiteDailyData> {
        Logger.d(TAG) { "calling site daily data" }
        val siteId = dataRepository.getSettings().firstOrNull()?.siteId
        return siteId?.let { id ->
            val now = Clock.System.now()
            val timeZone = TimeZone.of("Europe/Paris")
            val startOfDay = now.toLocalDateTime(timeZone).date.atStartOfDayIn(timeZone)

            dataRepository.api.fetchSiteTimeSeries(
                siteId = id,
                startTime = startOfDay,
                endTime = now,
                measureKind = MeasureKind.QUANTITY,
                aggregationLevel = AggregationLevel.NONE,
                aggregationType = AggregationType.SUM
            )
                .mapLeft { DomainError.Api(it) }
                .map { timeSeries ->
                    val lastUpdateTimestamp = if (timeSeries.timestamps.isNotEmpty()) {
                        Instant.parse(timeSeries.timestamps.last())
                    } else {
                        Instant.DISTANT_PAST
                    }

                    val totalProduction = timeSeries.productions.sum()
                    val totalConsumption = timeSeries.consumptions.sum()
                    val totalInjection = timeSeries.injections.sum()
                    val totalWithdrawals = timeSeries.withdrawals.sum()

                    // Calculate self-consumption rate: (production - injection) / production
                    val selfConsumptionRate = if (totalProduction > 0) {
                        ((totalProduction - totalInjection) / totalProduction).coerceIn(0.0, 1.0)
                    } else {
                        0.0
                    }

                    // Calculate autonomy rate: (consumption - withdrawals) / consumption
                    val autonomyRate = if (totalConsumption > 0) {
                        ((totalConsumption - totalWithdrawals) / totalConsumption).coerceIn(
                            0.0,
                            1.0
                        )
                    } else {
                        0.0
                    }

                    SiteDailyData(
                        totalProduction = totalProduction,
                        totalConsumption = totalConsumption,
                        totalInjection = totalInjection,
                        totalWithdrawals = totalWithdrawals,
                        selfConsumptionRate = selfConsumptionRate,
                        autonomyRate = autonomyRate,
                        lastUpdateTimestamp = lastUpdateTimestamp,
                        updateDate = lastUpdateTimestamp.format(DateTimeComponents.Formats.RFC_1123),
                        lastRefreshDate = Clock.System.now()
                            .format(DateTimeComponents.Formats.RFC_1123),
                    )
                }
        } ?: Either.Left(DomainError.Generic("Site id not found"))
    }

    companion object {
        private const val TAG = "FetchSiteDailyDataUseCase"
    }
}
