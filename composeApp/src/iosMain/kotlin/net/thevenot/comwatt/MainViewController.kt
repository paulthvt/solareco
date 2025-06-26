package net.thevenot.comwatt

import androidx.compose.ui.window.ComposeUIViewController
import net.thevenot.comwatt.di.Factory

fun MainViewController() = ComposeUIViewController {
    App(
        dynamicColor = false,
        AppContainer(Factory())
    )
}