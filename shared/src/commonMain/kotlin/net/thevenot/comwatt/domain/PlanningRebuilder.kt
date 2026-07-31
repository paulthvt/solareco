package net.thevenot.comwatt.domain

import co.touchlab.kermit.Logger
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.model.PlanningDeviceRefDto
import net.thevenot.comwatt.model.PlanningDto

/**
 * Builds the body for `PUT /api/plannings/{id}`.
 *
 * The endpoint **replaces** `typicalDaySchedules` wholesale: any schedule
 * missing from the body is deleted, and the server returns 200 either way. So
 * every surviving user schedule must be present, with its typical day inlined
 * in full (an id-only reference returns 500).
 *
 * Server-managed schedules (`optimalPlanning: true`) are deliberately excluded
 * — the server re-attaches its own copies and ignores any the client sends.
 */
object PlanningRebuilder {

    private const val TAG = "PlanningRebuilder"

    /**
     * @param current the planning as last read from the API, for its id and device
     * @param userSchedules the user-owned schedules that should survive the write
     * @param allowEmpty must be set explicitly to write an empty schedule list
     *   when the post-filter set is empty, so that an accidentally empty result
     *   cannot silently wipe a device's planning. The guard applies to the list
     *   after server-managed entries are removed.
     */
    fun buildWriteBody(
        current: PlanningDto,
        userSchedules: List<DeviceSchedule>,
        allowEmpty: Boolean = false,
    ): PlanningDto {
        val filtered = userSchedules.filterNot { it.isServerManaged }
        val dropped = userSchedules.size - filtered.size
        if (dropped > 0) {
            Logger.w(TAG) {
                "Dropped $dropped server-managed schedule(s) from write body for planning ${current.id}"
            }
        }

        require(filtered.isNotEmpty() || allowEmpty) {
            "Refusing to write an empty schedule list for planning ${current.id}: " +
                "PUT replaces the whole array and would delete every schedule. " +
                "Pass allowEmpty = true if the user really deleted all of them."
        }

        return PlanningDto(
            id = current.id,
            isDefault = current.isDefault,
            status = current.status,
            device = PlanningDeviceRefDto(id = current.device.id),
            typicalDaySchedules = filtered.map { it.toDto() },
        )
    }
}
