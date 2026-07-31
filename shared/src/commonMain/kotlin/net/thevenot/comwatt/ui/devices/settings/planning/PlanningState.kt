package net.thevenot.comwatt.ui.devices.settings.planning

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

    /** Schedules Comwatt generated: shown for explanation, never touched. */
    val serverSchedules: List<DeviceSchedule>
        get() = planning?.schedules?.filter { it.isServerManaged }.orEmpty()

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
