package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.model.PlanningDto

/** Everything the Planning tab needs, in one load. */
data class DevicePlanning(
    val planningId: Int?,
    val schedules: List<DeviceSchedule>,
    /** Typical days offered in the picker. Server-generated days are excluded. */
    val availableTypicalDays: List<TypicalDay>,
    /** How many devices on the site use each typical day, for the sharing warning. */
    val usageCountByTypicalDayId: Map<Int, Int>,
    /** The planning exactly as fetched, needed to build a write body. */
    val rawPlanning: PlanningDto?,
)

class FetchDevicePlanningUseCase(private val api: ComwattApi) {

    /**
     * The schedule list comes from the **site** endpoint, never from
     * `GET /api/plannings?deviceId=`: the device endpoint returns only the
     * schedules active today, and since `PUT /api/plannings/{id}` replaces the
     * whole array, writing back a today-only list silently deletes every
     * schedule whose date window lies outside it. The site endpoint returns all
     * of them, so a failure here is fatal rather than advisory.
     */
    suspend fun invoke(deviceId: Int, siteId: Int): Either<DomainError, DevicePlanning> {
        return try {
            val sitePlannings = api.fetchSitePlannings(siteId)
                .mapLeft { DomainError.Api(it) }
                .onLeft { Logger.e(TAG) { "Could not load site plannings: $it" } }
                .fold({ return Either.Left(it) }, { it.content })

            // A device with no planning yet is legitimate: the tab renders empty.
            val planning = sitePlannings.firstOrNull { it.device.id == deviceId }

            val typicalDays = api.fetchTypicalDays(siteId)
                .getOrNull()
                ?.content
                ?.map { it.toDomain() }
                ?.filterNot { it.isServerManaged }
                .orEmpty()

            val usage = sitePlannings.countTypicalDayUsage()

            Either.Right(
                DevicePlanning(
                    planningId = planning?.id,
                    schedules = planning?.typicalDaySchedules?.map { it.toDomain() }.orEmpty(),
                    availableTypicalDays = typicalDays,
                    usageCountByTypicalDayId = usage,
                    rawPlanning = planning,
                )
            )
        } catch (e: Exception) {
            Logger.e(TAG) { "Error fetching device planning: ${e.message}" }
            Either.Left(DomainError.Generic(e.message ?: "Unknown error"))
        }
    }

    /** Distinct device count per typical day id across every planning on the site. */
    private fun List<PlanningDto>.countTypicalDayUsage(): Map<Int, Int> =
        flatMap { planning ->
            planning.typicalDaySchedules.mapNotNull { schedule ->
                schedule.typicalDay.id?.let { it to planning.device.id }
            }
        }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, deviceIds) -> deviceIds.distinct().size }

    companion object {
        private const val TAG = "FetchDevicePlanningUseCase"
    }
}
