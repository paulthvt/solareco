package net.thevenot.comwatt.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import net.thevenot.comwatt.domain.FetchTimeSeriesUseCase
import net.thevenot.comwatt.domain.model.ChartTimeSeries

class DashboardViewModel(private val fetchTimeSeriesUseCase: FetchTimeSeriesUseCase): ViewModel() {
    private var autoRefreshJob: Job? = null

    private val _charts = MutableStateFlow<List<ChartTimeSeries>>(listOf())
    val charts: StateFlow<List<ChartTimeSeries>> = _charts

    fun load() {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        Napier.d(tag = TAG) { "startAutoRefresh ${this@DashboardViewModel}" }
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            fetchTimeSeriesUseCase.invoke()
                .flowOn(Dispatchers.IO)
                .catch {
                    Napier.e(tag = TAG) { "Error in auto refresh: $it" }
                }
                .collect {
                    _charts.value = it
                }
        }
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}