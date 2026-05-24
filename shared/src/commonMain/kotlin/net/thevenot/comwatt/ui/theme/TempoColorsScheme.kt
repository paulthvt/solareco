package net.thevenot.comwatt.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Tempo Blue day color
@Stable
val ColorScheme.tempoBlue: Color get() = if (isLight()) tempoBlueDayLight else tempoBlueDayDark

// Tempo White day color
@Stable
val ColorScheme.tempoWhite: Color get() = if (isLight()) tempoWhiteDayLight else tempoWhiteDayDark

// Tempo Red day color
@Stable
val ColorScheme.tempoRed: Color get() = if (isLight()) tempoRedDayLight else tempoRedDayDark

// Tempo Unknown day color (when tomorrow's color is not yet known)
@Stable
val ColorScheme.tempoUnknown: Color get() = if (isLight()) tempoUnknownDayLight else tempoUnknownDayDark

// Text colors for Tempo day chips
@Stable
val ColorScheme.tempoBlueText: Color get() = if (isLight()) tempoBlueTextLight else tempoBlueTextDark

@Stable
val ColorScheme.tempoWhiteText: Color get() = if (isLight()) tempoWhiteTextLight else tempoWhiteTextDark

@Stable
val ColorScheme.tempoRedText: Color get() = if (isLight()) tempoRedTextLight else tempoRedTextDark

@Stable
val ColorScheme.tempoUnknownText: Color get() = if (isLight()) tempoUnknownTextLight else tempoUnknownTextDark

private fun ColorScheme.isLight() = this.background.luminance() > 0.5f
