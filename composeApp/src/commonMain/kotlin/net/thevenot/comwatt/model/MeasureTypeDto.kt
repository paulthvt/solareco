package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MeasureTypeDto(
    @SerialName("@id")
    val atId: String?,
    val id: Int?,
    val code: String?,
    val measureKinds: List<String>?
)