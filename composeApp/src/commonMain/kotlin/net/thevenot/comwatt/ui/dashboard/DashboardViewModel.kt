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

    private val _uiState = MutableStateFlow(DashboardScreenState())
    val uiState: StateFlow<DashboardScreenState> get() = _uiState

    fun load() {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        Napier.d(tag = TAG) { "startAutoRefresh ${this@DashboardViewModel}" }
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            fetchTimeSeriesUseCase.invoke()
                .flowOn(Dispatchers.IO)
                .catch {
                    Napier.e(tag = TAG) { "Error in auto refresh: $it" }
                    _uiState.value = _uiState.value.copy(
                        errorCount = _uiState.value.errorCount + 1
                    )
                }
                .collect {
                    _charts.value = it
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _uiState.value = _uiState.value.copy(callCount = _uiState.value.callCount + 1)
                }
        }
    }

    fun singleRefresh() {
        viewModelScope.launch {
            Napier.d(tag = TAG) { "Single refresh ${this@DashboardViewModel}" }
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            fetchTimeSeriesUseCase.singleFetch().onRight {
                _charts.value = it
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    callCount = _uiState.value.callCount + 1)
            }
        }
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}