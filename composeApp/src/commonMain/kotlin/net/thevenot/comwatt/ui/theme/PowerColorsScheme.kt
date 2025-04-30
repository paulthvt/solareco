package net.thevenot.comwatt.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Production colors
@Stable
val ColorScheme.powerProduction: Color get() = if (isLight()) powerProductionLight else powerProductionDark

@Stable
val ColorScheme.powerProductionGaugeStart: Color get() = if (isLight()) powerProductionGaugeStartLight else powerProductionGaugeStartDark

@Stable
val ColorScheme.powerProductionGaugeEnd: Color get() = if (isLight()) powerProductionGaugeEndLight else powerProductionGaugeEndDark

// Consumption colors
@Stable
val ColorScheme.powerConsumption: Color get() = if (isLight()) powerConsumptionLight else powerConsumptionDark

@Stable
val ColorScheme.powerConsumptionGaugeStart: Color get() = if (isLight()) powerConsumptionGaugeStartLight else powerConsumptionGaugeStartDark

@Stable
val ColorScheme.powerConsumptionGaugeEnd: Color get() = if (isLight()) powerConsumptionGaugeEndLight else powerConsumptionGaugeEndDark

// Injection colors
@Stable
val ColorScheme.powerInjection: Color get() = if (isLight()) powerInjectionLight else powerInjectionDark

@Stable
val ColorScheme.powerInjectionGaugeStart: Color get() = if (isLight()) powerInjectionGaugeStartLight else powerInjectionGaugeStartDark

@Stable
val ColorScheme.powerInjectionGaugeEnd: Color get() = if (isLight()) powerInjectionGaugeEndLight else powerInjectionGaugeEndDark

// Withdrawal colors
@Stable
val ColorScheme.powerWithdrawals: Color get() = if (isLight()) powerWithdrawalsLight else powerWithdrawalsDark

@Stable
val ColorScheme.powerWithdrawalsGaugeStart: Color get() = if (isLight()) powerWithdrawalsGaugeStartLight else powerWithdrawalsGaugeStartDark

@Stable
val ColorScheme.powerWithdrawalsGaugeEnd: Color get() = if (isLight()) powerWithdrawalsGaugeEndLight else powerWithdrawalsGaugeEndDark

private fun ColorScheme.isLight() = this.background.luminance() > 0.5f