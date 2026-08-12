package net.thevenot.comwatt.ui.devices

import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.DeviceUiModel

data class DevicesScreenState(
    val isRefreshing: Boolean = false,
    val isDataLoaded: Boolean = false,
    val lastErrorMessage: String = "",
    val devices: List<DeviceUiModel> = emptyList(),
    /**
     * Optimistic control states, keyed by device id. An entry means a write is
     * in flight for that device; the card shows the entry instead of the
     * server value, and the control is disabled until it clears.
     */
    val pendingStates: Map<Int, DeviceControlState> = emptyMap(),
    /** Incremented on each control write failure, to re-trigger the snackbar. */
    val lastControlErrorId: Int = 0,
    /** Active schedules keyed by device id, used to render the AUTO summary line. */
    val schedulesByDeviceId: Map<Int, List<DeviceSchedule>> = emptyMap(),
    /**
     * Incremented each time a device load completes (success or error). Used to
     * re-key the clock `remember` in the UI so the schedule summary re-reads the
     * current time after every refresh rather than keeping the first-composition value.
     */
    val refreshCount: Int = 0,
)
