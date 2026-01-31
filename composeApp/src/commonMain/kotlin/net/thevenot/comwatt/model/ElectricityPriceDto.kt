package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ElectricityPriceResponseDto(
    val tempoSyntheses: TempoSynthesesDto,
    val daily: List<DailyElectricityPriceDto>,
    val tempoSynthesesComplete: Boolean
)

@Serializable
data class TempoSynthesesDto(
    @SerialName("WHITE")
    val white: TempoDaySynthesisDto,
    @SerialName("BLUE")
    val blue: TempoDaySynthesisDto,
    @SerialName("RED")
    val red: TempoDaySynthesisDto
)

@Serializable
data class TempoDaySynthesisDto(
    val numberOfDays: Int,
    val totalNumberOfDays: Int
)

@Serializable
data class DailyElectricityPriceDto(
    val date: String,
    val dayValue: TempoDayValue,
    val status: List<DayStatusDto>
)

@Serializable
data class DayStatusDto(
    val value: TempoDayValue,
    val type: PeakType,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String
)

@Serializable
enum class TempoDayValue {
    BLUE, WHITE, RED
}

@Serializable
enum class PeakType {
    PEAK, OFFPEAK
}
