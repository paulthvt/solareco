package net.thevenot.comwatt.model

import kotlinx.serialization.Serializable

@Serializable
data class Site(
    val id: Int,
    val name: String,
    val description: String?,
    val createDate: String,
    val updateDate: String,
    val ownerAssignDate: String,
    val threePhase: Boolean,
    val address: Address,
    val currency: String,
    val language: String,
    val metric: String,
    val timezone: String,
    val siteUid: String,
    val supplyNumber: String,
    val status: String,
    val owner: String?,
    val accessType: String,
    val state: String,
    val siteKind: String
)