package net.thevenot.comwatt.database

data class SolarEcoSettings(
    val siteId: Int?,
    val dashboardSelectedTimeUnitIndex: Int?,
    val maxPowerGauge: Int?,
    val productionNoiseThreshold: Int?,
    val dashboardHiddenDevices: Set<String>?,
    val dashboardSortMode: String?,
    val tariffConfigJson: String?
)
