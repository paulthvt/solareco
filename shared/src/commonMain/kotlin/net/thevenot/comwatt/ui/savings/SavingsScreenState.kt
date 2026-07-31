package net.thevenot.comwatt.ui.savings

import net.thevenot.comwatt.model.savings.SavingsBreakdown
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.ui.dashboard.SelectedTimeRange
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit

data class SavingsScreenState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasError: Boolean = false,
    val breakdown: SavingsBreakdown = SavingsBreakdown.EMPTY,
    val selectedTimeUnit: DashboardTimeUnit = DashboardTimeUnit.DAY,
    val selectedTimeRange: SelectedTimeRange = SelectedTimeRange(),
    val config: TariffConfig = TariffConfig.defaults(),
    val configConfirmed: Boolean = false,
)
