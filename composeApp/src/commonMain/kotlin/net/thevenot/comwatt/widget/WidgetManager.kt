package net.thevenot.comwatt.widget

import arrow.core.Either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.DataRepository

/**
 * Cross-platform widget manager
 */
class WidgetManager(
    private val dataRepository: DataRepository,
    private val platformWidgetUpdater: PlatformWidgetUpdater
) {
    private val logger = Logger.withTag("WidgetManager")

    /**
     * Update widget data across platforms
     */
    suspend fun updateWidgetData(siteId: Int): Either<String, Unit> {
        logger.d { "Updating widget data for site $siteId" }

        val fetchUseCase = FetchWidgetConsumptionUseCase(dataRepository)
        return when (val result = fetchUseCase.execute(siteId)) {
            is Either.Left -> {
                logger.e { "Failed to fetch widget data: ${result.value}" }
                Either.Left(result.value)
            }

            is Either.Right -> {
                try {
                    // Save data using platform-specific mechanism
                    platformWidgetUpdater.saveWidgetData(result.value)

                    // Trigger widget refresh
                    platformWidgetUpdater.refreshWidgets()

                    logger.d { "Widget data updated successfully" }
                    Either.Right(Unit)
                } catch (e: Exception) {
                    logger.e(e) { "Error updating widgets" }
                    Either.Left(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Schedule periodic widget updates
     */
    fun schedulePeriodicUpdates() {
        platformWidgetUpdater.schedulePeriodicUpdates()
    }

    /**
     * Cancel scheduled widget updates
     */
    fun cancelPeriodicUpdates() {
        platformWidgetUpdater.cancelPeriodicUpdates()
    }
}

/**
 * Platform-specific widget updater interface
 */
interface PlatformWidgetUpdater {
    suspend fun saveWidgetData(data: WidgetConsumptionData)
    fun refreshWidgets()
    fun schedulePeriodicUpdates()
    fun cancelPeriodicUpdates()
}
