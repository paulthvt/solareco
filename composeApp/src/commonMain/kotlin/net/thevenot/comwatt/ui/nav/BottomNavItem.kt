package net.thevenot.comwatt.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.bottom_nav_dashboard
import comwatt.composeapp.generated.resources.bottom_nav_devices
import comwatt.composeapp.generated.resources.bottom_nav_home
import comwatt.composeapp.generated.resources.bottom_nav_more
import kotlinx.serialization.Serializable
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import org.jetbrains.compose.resources.StringResource

@Serializable
enum class BottomNavItem(
    val label: StringResource,
    val screen: Screen,
) {
    Home(
        label = Res.string.bottom_nav_home,
        screen = Screen.Home,
    ),
    Dashboard(
        label = Res.string.bottom_nav_dashboard,
        screen = Screen.Dashboard,
    ),
    Devices(
        label = Res.string.bottom_nav_devices,
        screen = Screen.Devices,
    ),
    More(
        label = Res.string.bottom_nav_more,
        screen = Screen.More,
    );

    @Composable
    fun icon(): Painter = when (this) {
        Home -> AppIcons.Home
        Dashboard -> AppIcons.Dashboard
        Devices -> AppIcons.Power
        More -> AppIcons.Menu
    }
}