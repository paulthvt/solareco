package net.thevenot.comwatt.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.firstOrNull
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.model.DeviceSchedule

/**
 * Every device's schedules in one call, keyed by device id.
 *
 * Deliberately returns a bare map rather than an `Either`: the summary line this
 * feeds is advisory, so a failure means "no summary" and a logged warning, not
 * an error the devices list has to render. This is the only place in the
 * codebase that swallows an `ApiError` on purpose.
 */
class FetchSiteSchedulesUseCase(private val dataRepository: DataRepository) {

    suspend operator fun invoke(): Map<Int, List<DeviceSchedule>> {
        return try {
            val siteId = dataRepository.getSettings().firstOrNull()?.siteId ?: run {
                Logger.w(TAG) { "No site selected, cards render without summaries" }
                return emptyMap()
            }

            dataRepository.api.fetchSitePlannings(siteId).fold(
                ifLeft = { error ->
                    Logger.w(TAG) { "Site plannings unavailable, cards render without summaries: $error" }
                    emptyMap()
                },
                ifRight = { paged ->
                    paged.content.associate { planning ->
                        planning.device.id to planning.typicalDaySchedules.map { it.toDomain() }
                    }
                }
            )
        } catch (e: Exception) {
            Logger.w(TAG) { "Failed to parse site schedules, cards render without summaries: ${e.message}" }
            emptyMap()
        }
    }

    companion object {
        private const val TAG = "FetchSiteSchedulesUseCase"
    }
}
