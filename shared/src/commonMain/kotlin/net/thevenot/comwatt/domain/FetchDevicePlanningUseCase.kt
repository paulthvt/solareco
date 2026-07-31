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

    suspend fun invoke(deviceId: Int, siteId: Int): Either<DomainError, DevicePlanning> {
        return try {
            val planning = api.fetchPlannings(deviceId)
                .mapLeft { DomainError.Api(it) }
                .fold({ return Either.Left(it) }, { it.content.firstOrNull() })

            val typicalDays = api.fetchTypicalDays(siteId)
                .getOrNull()
                ?.content
                ?.map { it.toDomain() }
                ?.filterNot { it.isServerManaged }
                .orEmpty()

            // Advisory only: a failure here costs the "shared with N devices" lines.
            val usage = api.fetchSitePlannings(siteId)
                .onLeft { Logger.w(TAG) { "Could not load site plannings for sharing counts: $it" } }
                .getOrNull()
                ?.content
                ?.countTypicalDayUsage()
                .orEmpty()

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
