package net.thevenot.comwatt.ui.dashboard

data class DashboardScreenState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val callCount: Int = 0,
    val errorCount: Int = 0,
)
