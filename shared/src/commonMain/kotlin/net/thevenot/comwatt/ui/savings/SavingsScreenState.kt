package net.thevenot.comwatt.ui.savings

import net.thevenot.comwatt.model.savings.SavingsBreakdown
import net.thevenot.comwatt.model.savings.TariffConfig

// TODO(Task 7/8): Restore period field after SavingsPeriod is reimplemented for UI
data class SavingsScreenState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val breakdown: SavingsBreakdown = SavingsBreakdown.EMPTY,
    // val period: SavingsPeriod = SavingsPeriod.ThisMonth,
    val config: TariffConfig = TariffConfig.defaults(),
    val configConfirmed: Boolean = false,
)
