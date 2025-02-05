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
import net.thevenot.comwatt.domain.model.DeviceTimeSeries

class DashboardViewModel(private val fetchTimeSeriesUseCase: FetchTimeSeriesUseCase): ViewModel() {
    private var autoRefreshJob: Job? = null

    private val _devicesSeries = MutableStateFlow<List<DeviceTimeSeries>>(listOf())
    val devicesSeries: StateFlow<List<DeviceTimeSeries>> = _devicesSeries

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
                    _devicesSeries.value = it
                }
        }
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}