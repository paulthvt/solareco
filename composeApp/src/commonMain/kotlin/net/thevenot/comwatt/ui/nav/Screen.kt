package net.thevenot.comwatt.ui.nav

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object SiteChooser : Screen

    @Serializable
    data object Main : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object Devices : Screen

    @Serializable
    data object More : Screen

    @Serializable
    data object Login : Screen

    @Serializable
    data object UserSettings : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data class FullscreenChart(val chartIndex: Int) : Screen
}