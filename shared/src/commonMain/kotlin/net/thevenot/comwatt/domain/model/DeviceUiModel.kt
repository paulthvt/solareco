package net.thevenot.comwatt.domain.model

import net.thevenot.comwatt.model.DeviceCode

data class DeviceUiModel(
    val id: Int,
    val name: String,
    val deviceCode: DeviceCode?,
    val isOnline: Boolean,
    val isProduction: Boolean,
    val instantPowerWatts: Double?,
    val dailyEnergyWh: Double?,
    val hasToggle: Boolean,
    val isToggleEnabled: Boolean,
    val category: DeviceCategoryGroup,
)

enum class DeviceCategoryGroup {
    PRODUCTION,
    CONSUMPTION,
    GRID,
    STORAGE,
}
