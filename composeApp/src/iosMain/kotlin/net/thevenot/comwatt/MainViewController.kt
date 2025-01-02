package net.thevenot.comwatt

import androidx.compose.ui.window.ComposeUIViewController
import net.thevenot.comwatt.di.Factory
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

fun MainViewController() = ComposeUIViewController {
    val isDarkTheme =
        UIScreen.mainScreen.traitCollection.userInterfaceStyle ==
                UIUserInterfaceStyle.UIUserInterfaceStyleDark
    App(
        darkTheme = isDarkTheme,
        dynamicColor = false,
        AppContainer(Factory())
    )
}