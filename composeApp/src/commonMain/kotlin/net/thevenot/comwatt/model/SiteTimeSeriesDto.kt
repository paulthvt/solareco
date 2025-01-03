package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SiteTimeSeriesDto(
    val timestamps: List<String>,
    val productions: List<Double>,
    val consumptions: List<Double>,
    val injections: List<Double>,
    val withdrawals: List<Double>,
    val charges: List<Double>,
    val discharges: List<Double>,
    @SerialName("autoproductionRates")
    val autoProductionRates: List<Double>,
    @SerialName("autoconsumptionRates")
    val autoConsumptionRates: List<Double>,
    val injectionRates: List<Double>,
    val withdrawalRates: List<Double>
)