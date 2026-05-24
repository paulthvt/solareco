package net.thevenot.comwatt.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.thevenot.comwatt.model.type.MeasureKind

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
    val atClass: String? = null,
    @SerialName("@id")
    val atId: String? = null,
    @SerialName("@ref")
    val atRef: String? = null,
    val id: Int? = null,
    val measureKind: MeasureKind? = null,
    val measureType: MeasureTypeDto? = null,
    val measureKey: String? = null,
    val shared: Boolean? = null,
    val device: DeviceDto? = null
)

@Serializable
enum class TileType {
    THIRD_PARTY, VALUATION
}