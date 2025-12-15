package net.thevenot.comwatt.widget

import arrow.core.Either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchWidgetConsumptionUseCase
import net.thevenot.comwatt.domain.exception.DomainError

class WidgetManager(
    private val dataRepository: DataRepository,
    private val platformWidgetUpdater: PlatformWidgetUpdater
) {
    private val logger = Logger.withTag("WidgetManager")

    suspend fun updateWidgetData(siteId: Int): Either<DomainError, Unit> {
        logger.d { "Updating widget data for site $siteId" }

        val fetchUseCase = FetchWidgetConsumptionUseCase(dataRepository)
        return when (val result = fetchUseCase.execute(siteId)) {
            is Either.Left -> {
                logger.e { "Failed to fetch widget data: ${result.value}" }
                Either.Left(result.value)
            }
            is Either.Right -> {
                try {
                    platformWidgetUpdater.saveWidgetData(result.value)
                    platformWidgetUpdater.refreshWidgets()
                    logger.d { "Widget data updated successfully" }
                    Either.Right(Unit)
                } catch (e: Exception) {
                    logger.e(e) { "Error updating widgets" }
                    Either.Left(DomainError.Generic(e.message ?: "Unknown error"))
                }
            }
        }
    }

    fun schedulePeriodicUpdates() {
        platformWidgetUpdater.schedulePeriodicUpdates()
    }

    fun cancelPeriodicUpdates() {
        platformWidgetUpdater.cancelPeriodicUpdates()
    }
}

interface PlatformWidgetUpdater {
    suspend fun saveWidgetData(data: WidgetConsumptionData)
    fun refreshWidgets()
    fun schedulePeriodicUpdates()
    fun cancelPeriodicUpdates()
}