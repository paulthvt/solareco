package net.thevenot.comwatt.model
import kotlinx.serialization.Serializable

@Serializable
data class TimeSeriesDto(
    val timestamps: List<String>,
    val values: List<Double>
)