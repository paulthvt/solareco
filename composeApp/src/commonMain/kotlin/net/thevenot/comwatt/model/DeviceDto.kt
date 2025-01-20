package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    @SerialName("@class")
    val atClass: String?,
    @SerialName("@id")
    val atId: String?,
    val sourceIsOnline: Boolean?,
    val features: List<FeatureDto>?,
    val id: Int?,
    val name: String?,
    val site: SiteDto?,
    val deviceKind: DeviceKind?,
    val configuration: ConfigurationDto?,
    val capacities: List<CapacityDto>?,
    val archived: Boolean?,
    val coState: CoState?,
    val partNature: PartNature?,
    val threePhase: Boolean?,
    val partChilds: List<DeviceDto>?,
    val partKind: DeviceKind?,
    val partChild: Boolean?,
    val global: Boolean?,
    val production: Boolean?
)

@Serializable
data class DeviceKind(
    @SerialName("@id")
    val atId: String?,
    val id: Int?,
    val code: DeviceCode?,
    val global: Boolean?,
    val icon: String?,
    val production: Boolean?,
    val onlyInfo: Boolean?,
    val injection: Boolean?,
    val withdrawal: Boolean?,
    val displayOrder: Int?,
    val category: DeviceCategory?,
    val features: String?,
    val codeEnum: DeviceCode?,
    val eligibleToSgReady: Boolean?,
    val partKindId: Int?
)

@Serializable
enum class CoState {
    WORKING, NOT_WORKING
}

@Serializable
enum class PartNature {
    COMPOSITE_DEVICE, DEVICE
}

@Serializable
enum class DeviceCode {
    GRID_METER, WITHDRAWAL, INJECTION, WASHING_MACHINE, PRO_POWER_OUTLET, DISH_WASHER, SOLAR_PANEL, PRO_HEAT_PUMP, OVEN, HOUSEHOLD_APPLIANCES
}

@Serializable
enum class DeviceCategory {
    TOTAL_CONSUMPTION, ELECTRICAL_APPLIANCES, PROFESSIONAL_EQUIPMENT, SOLAR_PRODUCTION
}