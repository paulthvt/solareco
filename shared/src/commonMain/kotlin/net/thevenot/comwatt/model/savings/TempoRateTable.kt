package net.thevenot.comwatt.model.savings

import kotlinx.serialization.Serializable

@Serializable
data class TempoRateTable(
    val blueHp: Double, val blueHc: Double,
    val whiteHp: Double, val whiteHc: Double,
    val redHp: Double, val redHc: Double,
)
