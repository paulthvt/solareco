package net.thevenot.comwatt.domain.model

import kotlinx.serialization.json.JsonElement

data class DeviceDetail(
    val id: Int,
    val name: String,
    val deviceKindCode: String?,
    val rawJson: JsonElement,
)
