package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.utils.computeSiteStats
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.AggregationType
import net.thevenot.comwatt.model.type.MeasureKind
import net.thevenot.comwatt.ui.settings.SettingsViewModel.Companion.DEFAULT_PRODUCTION_NOISE_THRESHOLD
import net.thevenot.comwatt.widget.WidgetStatisticsData
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class FetchWidgetStatisticsUseCase(
    private val dataRepository: DataRepository,
) {
    private val logger = Logger.withTag("FetchWidgetStatisticsUseCase")

    suspend fun execute(siteId: Int): Either<DomainError, WidgetStatisticsData> {
        return try {
            val settings = dataRepository.getSettings().first()
            val productionNoiseThreshold =
                settings.productionNoiseThreshold ?: DEFAULT_PRODUCTION_NOISE_THRESHOLD

            val now = Clock.System.now()
            val oneHourAgo = now - 1.hours

            val response = dataRepository.api.fetchSiteTimeSeries(
                siteId = siteId,
                startTime = oneHourAgo,
                endTime = now,
                measureKind = MeasureKind.QUANTITY,
                aggregationLevel = AggregationLevel.NONE,
                aggregationType = AggregationType.SUM,
            )

            response.mapLeft { apiError ->
                logger.e { "Failed to fetch widget statistics: $apiError" }

                if (apiError is ApiError.HttpError && apiError.code == 401) {
                    logger.d { "Got 401, attempting auto login" }
                    dataRepository.tryAutoLogin({}, {})
                }

                DomainError.Api(apiError)
            }.map { timeSeries ->
                val lastTimestamp = if (timeSeries.timestamps.isNotEmpty()) {
                    Instant.parse(timeSeries.timestamps.last())
                } else {
                    Instant.DISTANT_PAST
                }

                val stats = computeSiteStats(
                    productions = timeSeries.productions,
                    consumptions = timeSeries.consumptions,
                    injections = timeSeries.injections,
                    withdrawals = timeSeries.withdrawals,
                    productionNoiseThreshold = productionNoiseThreshold,
                    lastTimestamp = lastTimestamp,
                )

                WidgetStatisticsData(
                    selfConsumptionRate = stats.selfConsumptionRate,
                    autonomyRate = stats.autonomyRate,
                    totalProduction = stats.totalProduction,
                    totalConsumption = stats.totalConsumption,
                    totalInjection = stats.totalInjection,
                    totalWithdrawals = stats.totalWithdrawals,
                    lastUpdateTime = Clock.System.now().toEpochMilliseconds(),
                )
            }
        } catch (e: Exception) {
            logger.e(e) { "Exception while fetching widget statistics" }
            Either.Left(DomainError.Generic(e.message ?: "Unknown error"))
        }
    }
}
