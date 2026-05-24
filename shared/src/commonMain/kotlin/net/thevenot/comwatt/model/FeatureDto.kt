package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeatureDto(
    @SerialName("@id")
    val atId: String,
    val id: Int,
    val feature: FeatureDetailDto,
    val enabled: Boolean,
    val capacities: List<CapacityDto>?
)

@Serializable
data class FeatureDetailDto(
    @SerialName("@id")
    val atId: String?,
    val id: Int?,
    val code: String?,
    val featureName: String?
)