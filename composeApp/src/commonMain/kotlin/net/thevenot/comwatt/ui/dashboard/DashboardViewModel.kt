package net.thevenot.comwatt.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchTimeSeriesUseCase
import net.thevenot.comwatt.domain.model.ChartTimeSeries
import net.thevenot.comwatt.domain.model.TimeUnit
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit

class DashboardViewModel(
    private val fetchTimeSeriesUseCase: FetchTimeSeriesUseCase,
    private val dataRepository: DataRepository
): ViewModel() {
    private var autoRefreshJob: Job? = null
    private val _charts = MutableStateFlow<List<ChartTimeSeries>>(listOf())

    val charts: StateFlow<List<ChartTimeSeries>> = _charts
    private val _uiState = MutableStateFlow(DashboardScreenState())

    val uiState: StateFlow<DashboardScreenState> get() = _uiState
    private var selectedTimeUnit =
        convertSelectedIndexToTimeUnit(_uiState.value.timeUnitSelectedIndex)

    fun startAutoRefresh() {
        Napier.d(tag = TAG) { "startAutoRefresh ${this@DashboardViewModel}" }
        if (autoRefreshJob?.isActive == true) return
        _uiState.value = _uiState.value.copy(isLoading = true)
        autoRefreshJob = viewModelScope.launch {
            val selectedTimeUnitIndex =  dataRepository.getSettings().firstOrNull()?.dashboardSelectedTimeUnitIndex
            Napier.d(tag = TAG) { "startAutoRefresh selectedTimeUnitIndex $selectedTimeUnitIndex" }
            selectedTimeUnitIndex?.let {
                _uiState.value = _uiState.value.copy(timeUnitSelectedIndex = it)
                selectedTimeUnit = convertSelectedIndexToTimeUnit(it)
                Napier.d(tag = TAG) { "startAutoRefresh selectedTimeUnit $selectedTimeUnit" }
            }

            fetchTimeSeriesUseCase.invoke(
                when (selectedTimeUnit) {
                    DashboardTimeUnit.HOUR -> TimeUnit.HOUR
                    DashboardTimeUnit.DAY -> TimeUnit.DAY
                    DashboardTimeUnit.WEEK -> TimeUnit.WEEK
                    DashboardTimeUnit.CUSTOM -> TODO() // todo handle custom
                }
            )
                .flowOn(Dispatchers.IO)
                .collect {
                    it.onLeft { error ->
                        Napier.e(tag = TAG) { "Error in auto refresh: $error" }
                        _uiState.value = _uiState.value.copy(
                            errorCount = _uiState.value.errorCount + 1
                        )
                    }
                    it.onRight { value ->
                        _charts.value = value
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _uiState.value = _uiState.value.copy(callCount = _uiState.value.callCount + 1)
                    }
                }
        }
    }

    fun stopAutoRefresh() {
        Napier.d(tag = TAG) { "stopAutoRefresh" }
        autoRefreshJob?.cancel()
    }

    fun singleRefresh() {
        viewModelScope.launch {
            Napier.d(tag = TAG) { "Single refresh $selectedTimeUnit" }
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            fetchTimeSeriesUseCase.singleFetch(
                when (selectedTimeUnit) {
                    DashboardTimeUnit.HOUR -> TimeUnit.HOUR
                    DashboardTimeUnit.DAY -> TimeUnit.DAY
                    DashboardTimeUnit.WEEK -> TimeUnit.WEEK
                    DashboardTimeUnit.CUSTOM -> TimeUnit.WEEK // todo handle custom
                }
            ).onRight {
                _charts.value = it
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    callCount = _uiState.value.callCount + 1)
            }
        }
    }

    fun onTimeUnitSelected(timeUnitSelectedIndex: Int) {
        _uiState.value = _uiState.value.copy(timeUnitSelectedIndex = timeUnitSelectedIndex)
        selectedTimeUnit = convertSelectedIndexToTimeUnit(timeUnitSelectedIndex)
        viewModelScope.launch {
            dataRepository.saveDashboardSelectedTimeUnitIndex(timeUnitSelectedIndex)
            stopAutoRefresh()
            startAutoRefresh()
        }
    }

    private fun convertSelectedIndexToTimeUnit(timeUnitSelectedIndex: Int): DashboardTimeUnit {
        return DashboardTimeUnit.entries[timeUnitSelectedIndex]
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}