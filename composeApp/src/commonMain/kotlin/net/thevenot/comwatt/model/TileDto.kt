package net.thevenot.comwatt.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TileResponseDto(
    val tileType: TileType,
    @SerialName("@id")
    val atId: String,
    val id: Int,
    val name: String,
    val site: SiteDto?,
    val position: Int,
    val chartType: String,
    val tileChartDatas: List<TileChartDataDto>? = null
)

@Serializable
data class TileChartDataDto(
    @SerialName("@id")
    val atId: String,
    val id: Int,
    val measureKey: MeasureKeyDto,
    val color: String
)

@Serializable
data class MeasureKeyDto(
    @SerialName("@class")
    val atClass: String,
    @SerialName("@id")
    val atId: String,
    val id: Int,
    val measureKind: String,
    val measureType: MeasureTypeDto,
    val measureKey: String,
    val shared: Boolean,
    val device: DeviceDto?
)

@Serializable
enum class TileType {
    THIRD_PARTY, VALUATION
}