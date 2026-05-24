package net.thevenot.comwatt.widget

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchWidgetConsumptionUseCase

private val logger = Logger.withTag("IosWidgetHelper")

fun updateWidgetData(dataRepository: DataRepository) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val settings = dataRepository.getSettings().first()
            val siteId = settings.siteId ?: return@launch

            val fetchUseCase = FetchWidgetConsumptionUseCase(dataRepository)
            when (val result = fetchUseCase.execute(siteId)) {
                is Either.Left -> logger.e { "Failed to fetch widget data: ${result.value}" }
                is Either.Right -> IosWidgetDataManager.saveWidgetData(result.value)
            }
        } catch (e: Exception) {
            logger.e(e) { "Exception updating widget data" }
        }
    }
}
