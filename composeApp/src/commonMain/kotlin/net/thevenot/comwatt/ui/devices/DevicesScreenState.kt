package net.thevenot.comwatt.ui.devices

import net.thevenot.comwatt.domain.model.DeviceUiModel

data class DevicesScreenState(
    val isRefreshing: Boolean = false,
    val isDataLoaded: Boolean = false,
    val lastErrorMessage: String = "",
    val devices: List<DeviceUiModel> = emptyList(),
)
