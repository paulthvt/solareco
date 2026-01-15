package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.TimeAgoUnit
import net.thevenot.comwatt.widget.WidgetConsumptionData
import kotlin.time.Clock
import kotlin.time.Instant

class FetchWidgetConsumptionUseCase(
    private val dataRepository: DataRepository
) {
    private val logger = Logger.withTag("FetchWidgetConsumptionUseCase")

    suspend fun execute(siteId: Int): Either<DomainError, WidgetConsumptionData> {
        return try {
            val response = dataRepository.api.fetchSiteTimeSeries(
                siteId = siteId,
                timeAgoUnit = TimeAgoUnit.HOUR,
                timeAgoValue = 1,
                aggregationLevel = AggregationLevel.NONE
            )

            response.mapLeft { apiError ->
                logger.e { "Failed to fetch widget data: $apiError" }

                if (apiError is ApiError.HttpError && apiError.code == 401) {
                    logger.d { "Got 401, attempting auto login" }
                    dataRepository.tryAutoLogin({}, {})
                }

                DomainError.Api(apiError)
            }.map { siteTimeSeries ->
                val timestamps = siteTimeSeries.timestamps.map { timestamp: String ->
                    try {
                        Instant.parse(timestamp).toEpochMilliseconds()
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to parse timestamp: $timestamp" }
                        0L
                    }
                }

                val consumptions = siteTimeSeries.consumptions
                val productions = siteTimeSeries.productions

                val maxConsumption = consumptions.maxOrNull() ?: 0.0
                val averageConsumption = if (consumptions.isNotEmpty()) {
                    consumptions.average()
                } else {
                    0.0
                }

                val maxProduction = productions.maxOrNull() ?: 0.0
                val averageProduction = if (productions.isNotEmpty()) {
                    productions.average()
                } else {
                    0.0
                }

                WidgetConsumptionData(
                    timestamps = timestamps,
                    consumptions = consumptions,
                    productions = productions,
                    lastUpdateTime = Clock.System.now().toEpochMilliseconds(),
                    maxConsumption = maxConsumption,
                    averageConsumption = averageConsumption,
                    maxProduction = maxProduction,
                    averageProduction = averageProduction
                )
            }
        } catch (e: Exception) {
            logger.e(e) { "Exception while fetching widget data" }
            Either.Left(DomainError.Generic(e.message ?: "Unknown error"))
        }
    }
}
