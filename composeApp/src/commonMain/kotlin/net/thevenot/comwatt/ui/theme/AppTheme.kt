package net.thevenot.comwatt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
expect fun ComwattTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
)

private val LocalAppDimens = staticCompositionLocalOf {
    normalDimensions
}

object AppTheme {
    val dimens: Dimensions
        @Composable
        get() = LocalAppDimens.current
}