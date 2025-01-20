package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CapacityDto(
    @SerialName("@id")
    val atId: String?,
    val id: Int?,
    val capacity: CapacityDetailDto?
)

@Serializable
data class CapacityDetailDto(
    @SerialName("@id")
    val atId: String?,
    val id: Int?,
    val capacityId: String?,
    val type: String?,
    val nature: String?,
    val sgReady: Boolean?,
    val instance: String?,
    val connectedObjectId: Int?,
    val measureKinds: List<String>?,
    val measureType: MeasureTypeDto?,
    val nativeMeasureType: Boolean?,
    val global: Boolean?,
    val production: Boolean?,
    val enable: Boolean?,
    val tadoCapacity: String?,
    val selectValues: String?,
    val calibration: Int?
)