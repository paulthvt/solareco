package net.thevenot.comwatt.ui.devices

import net.thevenot.comwatt.domain.model.DeviceControlState
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
)
