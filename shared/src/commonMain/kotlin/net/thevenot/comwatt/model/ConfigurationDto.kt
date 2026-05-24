package net.thevenot.comwatt.model

import kotlinx.serialization.Serializable

@Serializable
data class ConfigurationDto(
    val triggeringPower: String?,
    val maxPower: String?,
    val maxAutonomy: String?,
    val maxTimeCharge: String?,
    val interruption: Boolean?,
    val standbyValue: String?,
    val inversionOnOff: String?,
    val standbyDuration: String?,
    val rtClass: String?,
    val brand: String?,
    val model: String?,
    val power: String?,
    val moduleBrand: String?,
    val moduleModel: String?,
    val inverterBrand: String?,
    val inverterModel: String?,
    val generatorOrientation: String?,
    val efficiency: String?,
    val technology: String?,
    val resalePrice: String?,
    val flowRate: String?,
    val controlMode: String,
    val measureType: String?
)