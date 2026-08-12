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
     * The schedule Comwatt generated: shown for explanation, never touched.
     *
     * At most one is listed. Comwatt regenerates on a rolling seven-day window
     * and the site endpoint returns every past generation, so the raw list holds
     * both months-old windows and several current ones that overlap today —
     * which showed up as the same "Comwatt automatic" card two or three times.
     * The newest window that has not ended is the one that describes what the
     * device is doing now, so that is the one kept.
     *
     * Filtering is display-only: the write body has always excluded
     * server-managed schedules regardless.
     */
    fun serverSchedules(today: LocalDate): List<DeviceSchedule> =
        planning?.schedules
            ?.filter { it.isServerManaged && it.endDate >= today }
            ?.maxWithOrNull(compareBy({ it.startDate }, { it.endDate }))
            ?.let { listOf(it) }
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
