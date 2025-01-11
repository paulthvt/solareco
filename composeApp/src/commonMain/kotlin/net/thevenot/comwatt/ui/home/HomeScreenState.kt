package net.thevenot.comwatt.ui.home

data class HomeScreenState(
    val isLoading: Boolean = false,
    val callCount: Int = 0,
    val errorCount: Int = 0,
    val production: Double = Double.NaN,
    val consumption: Double = Double.NaN,
    val injection: Double = Double.NaN,
    val withdrawals: Double = Double.NaN,
    val consumptionRate: Double = Double.NaN,
    val productionRate: Double = Double.NaN,
    val injectionRate: Double = Double.NaN,
    val withdrawalsRate: Double = Double.NaN,
    val updateDate: String = "",
    val lastRefreshDate: String = ""
)
