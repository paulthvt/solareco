package net.thevenot.comwatt.widget

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.TimeAgoUnit

/**
 * Use case to fetch consumption data for widgets
 */
class FetchWidgetConsumptionUseCase(
    private val dataRepository: DataRepository
) {
    private val logger = Logger.withTag("FetchWidgetConsumptionUseCase")

    /**
     * Fetch last hour consumption data for widget display
     */
    suspend fun execute(siteId: Int): Either<String, WidgetConsumptionData> {
        return try {
            logger.d { "Fetching widget consumption data for site $siteId" }

            // Fetch time series data for the last hour
            val response = dataRepository.api.fetchSiteTimeSeries(
                siteId = siteId,
                timeAgoUnit = TimeAgoUnit.HOUR,
                timeAgoValue = 1,
                aggregationLevel = AggregationLevel.NONE // Raw data points
            )

            response.mapLeft { apiError ->
                val errorMessage = apiError.toString()
                logger.e { "Failed to fetch widget data: $errorMessage" }
                errorMessage
            }.map { siteTimeSeries ->
                // Convert timestamps to epoch milliseconds
                val timestamps = siteTimeSeries.timestamps.map { timestamp: String ->
                    try {
                        Instant.parse(timestamp).toEpochMilliseconds()
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to parse timestamp: $timestamp" }
                        0L
                    }
                }

                val consumptions = siteTimeSeries.consumptions

                // Calculate statistics
                val maxConsumption = consumptions.maxOrNull() ?: 0.0
                val averageConsumption = if (consumptions.isNotEmpty()) {
                    consumptions.average()
                } else {
                    0.0
                }

                val widgetData = WidgetConsumptionData(
                    timestamps = timestamps,
                    consumptions = consumptions,
                    lastUpdateTime = Clock.System.now().toEpochMilliseconds(),
                    maxConsumption = maxConsumption,
                    averageConsumption = averageConsumption
                )

                logger.d { "Widget data fetched successfully: ${consumptions.size} data points" }
                widgetData
            }
        } catch (e: Exception) {
            logger.e(e) { "Exception while fetching widget data" }
            Either.Left(e.message ?: "Unknown error")
        }
    }
}
