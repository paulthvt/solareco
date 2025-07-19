package net.thevenot.comwatt

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.thevenot.comwatt.di.Factory

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KotlinProject",
    ) {
        App(appContainer = AppContainer(Factory()))
    }
}