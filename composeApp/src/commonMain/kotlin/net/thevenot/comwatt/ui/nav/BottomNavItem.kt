package net.thevenot.comwatt.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Power
import androidx.compose.ui.graphics.vector.ImageVector
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.bottom_nav_dashboard
import comwatt.composeapp.generated.resources.bottom_nav_devices
import comwatt.composeapp.generated.resources.bottom_nav_home
import comwatt.composeapp.generated.resources.bottom_nav_more
import kotlinx.serialization.Serializable
import net.thevenot.comwatt.client.Session
import org.jetbrains.compose.resources.StringResource

@Serializable
enum class BottomNavItem(
    val icon: ImageVector,
    val label: StringResource,
    val screen: (Session?) -> Screen,
) {
    Home(
        icon = Icons.Default.Home,
        label = Res.string.bottom_nav_home,
        screen = { session -> Screen.Home(session) },
    ),
    Dashboard(
        icon = Icons.Default.Dashboard,
        label = Res.string.bottom_nav_dashboard,
        screen = { session -> Screen.Dashboard(session) },
    ),
    Devices(
        icon = Icons.Default.Power,
        label = Res.string.bottom_nav_devices,
        screen = { session -> Screen.Devices(session) },
    ),
    More(
        icon = Icons.Default.Menu,
        label = Res.string.bottom_nav_more,
        screen = { session -> Screen.More(session) },
    ),
}