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
    /** Capacity id for the switch endpoint; null when the device has no switch. */
    val switchCapacityId: Int? = null,
    val controlMode: ControlMode = ControlMode.MANUAL,
    /** The POWER_SWITCH capacity's real `enable` state. */
    val isSwitchOn: Boolean = false,
    val category: DeviceCategoryGroup,
)

enum class DeviceCategoryGroup {
    PRODUCTION,
    CONSUMPTION,
    GRID,
    STORAGE,
}
