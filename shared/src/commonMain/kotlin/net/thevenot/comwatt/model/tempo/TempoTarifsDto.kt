package net.thevenot.comwatt.model.tempo

import kotlinx.serialization.Serializable

@Serializable
data class TempoTarifsDto(
    val bleuHC: Double,
    val bleuHP: Double,
    val blancHC: Double,
    val blancHP: Double,
    val rougeHC: Double,
    val rougeHP: Double,
)
