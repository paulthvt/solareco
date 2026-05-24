package net.thevenot.comwatt.ui.theme

import androidx.compose.runtime.Composable

@Composable
actual fun ComwattTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    AppComwattTheme(
        colorScheme = if (darkTheme) darkScheme else lightScheme,
        typography = AppTypography,
        content = content
    )
}