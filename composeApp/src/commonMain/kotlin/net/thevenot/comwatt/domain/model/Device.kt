package net.thevenot.comwatt.domain.model

import androidx.compose.ui.graphics.vector.ImageVector

data class Device(
    val name: String,
    val kind: DeviceKind,
)

data class DeviceKind(
    val icon: ImageVector,
)