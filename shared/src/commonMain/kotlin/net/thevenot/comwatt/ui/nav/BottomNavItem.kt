package net.thevenot.comwatt.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.bottom_nav_dashboard
import comwatt.shared.generated.resources.bottom_nav_devices
import comwatt.shared.generated.resources.bottom_nav_home
import comwatt.shared.generated.resources.bottom_nav_savings
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
    Savings(
        label = Res.string.bottom_nav_savings,
        screen = Screen.Savings,
    ),
    Devices(
        label = Res.string.bottom_nav_devices,
        screen = Screen.Devices,
    );

    @Composable
    fun icon(): Painter = when (this) {
        Home -> AppIcons.Home
        Dashboard -> AppIcons.Dashboard
        Savings -> AppIcons.SolarPower
        Devices -> AppIcons.Power
    }
}