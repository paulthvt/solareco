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
    GRID_METER, WITHDRAWAL, INJECTION, WASHING_MACHINE, PRO_POWER_OUTLET, DISH_WASHER, SOLAR_PANEL, PRO_HEAT_PUMP, OVEN, HOUSEHOLD_APPLIANCES,
    PRO_ROOM_AIR_HANDLING_UNIT, PRO_COLD_UNIT, PRO_COMPRESSOR, PRO_LIGHT, PRO_HOT_WATER_TANK, PRO_BOILER, PRO_AIR_CONDITIONING, PRO_COLD_ROOM,
    PRO_ELECTRIC_VEHICLE, GLOBAL_CONSUMPTION, SOLAR_PANEL_RESALE, RADIATOR, TOWEL_DRYER, HEAT_PUMP, AIR_CONDITIONING, BOILER, HOT_WATER_TANK,
    HOT_WATER_TANK_THERM, CLOTHES_DRYER, FRIDGE, FREEZER, COFFEE_MACHINE, MICROWAVE_OVEN, TV, HI_FI, POOL, ELECTRIC_CAR, COMPUTER, LAPTOP, VMC,
    LIGHT, OTHER, INFO_ELECTRIC, BATTERY, BATTERY_CHARGE, BATTERY_DISCHARGE
}

@Serializable
enum class DeviceCategory {
    TOTAL_CONSUMPTION, ELECTRICAL_APPLIANCES, PROFESSIONAL_EQUIPMENT, SOLAR_PRODUCTION, HEATING, HEATING_WATER, MISC, ENERGY_STORAGE
}