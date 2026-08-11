package net.thevenot.comwatt.ui.devices.settings.planning

import kotlinx.datetime.LocalDate
import net.thevenot.comwatt.domain.DevicePlanning
import net.thevenot.comwatt.domain.model.DeviceSchedule

data class PlanningState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val errorMessage: String = "",
    val isSaving: Boolean = false,
    val planning: DevicePlanning? = null,
) {
    /** Schedules the user owns: editable and deletable. */
    val userSchedules: List<DeviceSchedule>
        get() = planning?.schedules?.filterNot { it.isServerManaged }.orEmpty()

    /**
     * Schedules Comwatt generated: shown for explanation, never touched.
     *
     * Only those still relevant on [today] are listed. Comwatt generates one per
     * week and the site endpoint returns every past one, so an unfiltered list
     * showed several identical "Comwatt automatic" cards whose windows had
     * expired months earlier. Expired ones are hidden from display only — the
     * write body has always excluded server-managed schedules regardless.
     */
    fun serverSchedules(today: LocalDate): List<DeviceSchedule> =
        planning?.schedules
            ?.filter { it.isServerManaged && it.endDate >= today }
            .orEmpty()

    /**
     * How many *other* devices use this typical day. The site-wide count
     * includes this device, so one is subtracted; zero means unshared, unknown,
     * or that the site plannings call failed.
     */
    fun sharingCount(typicalDayId: Int?): Int {
        val total = typicalDayId?.let { planning?.usageCountByTypicalDayId?.get(it) } ?: 0
        return (total - 1).coerceAtLeast(0)
    }
}
