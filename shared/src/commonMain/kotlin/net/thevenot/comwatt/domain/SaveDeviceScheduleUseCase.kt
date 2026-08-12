package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.model.PlanningDto

/**
 * Writes a device's schedules. The PUT replaces the whole array, so
 * [PlanningRebuilder] builds the body from every surviving user schedule.
 *
 * Schedule ids are reassigned by the server, so the response is mapped back and
 * returned — the caller must replace its list rather than keep the old ids.
 */
class SaveDeviceScheduleUseCase(private val api: ComwattApi) {

    suspend fun invoke(
        current: PlanningDto,
        schedules: List<DeviceSchedule>,
        allowEmpty: Boolean = false,
    ): Either<DomainError, List<DeviceSchedule>> {
        val body = try {
            PlanningRebuilder.buildWriteBody(current, schedules, allowEmpty)
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG) { "Refused to write planning ${current.id}: ${e.message}" }
            return Either.Left(DomainError.Generic(e.message ?: "Invalid schedule list"))
        }

        return api.updatePlanning(current.id, body)
            .mapLeft { DomainError.Api(it) as DomainError }
            .map { saved -> saved.typicalDaySchedules.map { it.toDomain() } }
            .onLeft { Logger.e(TAG) { "Failed to save planning ${current.id}: $it" } }
    }

    companion object {
        private const val TAG = "SaveDeviceScheduleUseCase"
    }
}
