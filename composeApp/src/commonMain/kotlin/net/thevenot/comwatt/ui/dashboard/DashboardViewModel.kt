package net.thevenot.comwatt.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchParameters
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

    fun startAutoRefresh() {
        Logger.d(TAG) { "startAutoRefresh ${this@DashboardViewModel}" }
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            val selectedTimeUnitIndex =
                dataRepository.getSettings().firstOrNull()?.dashboardSelectedTimeUnitIndex
            Logger.d(TAG) { "startAutoRefresh selectedTimeUnitIndex $selectedTimeUnitIndex" }
            selectedTimeUnitIndex?.let { index ->
                val timeUnit = DashboardTimeUnit.entries.getOrNull(index) ?: DashboardTimeUnit.HOUR
                _uiState.update { it.copy(selectedTimeUnit = timeUnit) }
                Logger.d(TAG) { "startAutoRefresh selectedTimeUnit ${_uiState.value.selectedTimeUnit}" }
            }

            fetchTimeSeriesUseCase.invoke {
                _uiState.update {
                    it.copy(selectedTimeRange = it.selectedTimeRange.withUpdatedHourRange())
                }
                Logger.d(TAG) { "startAutoRefresh selectedTimeRange ${_uiState.value.selectedTimeRange}" }

                FetchParameters(
                    timeUnit = when (_uiState.value.selectedTimeUnit) {
                        DashboardTimeUnit.HOUR -> TimeUnit.HOUR
                        DashboardTimeUnit.DAY -> TimeUnit.DAY
                        DashboardTimeUnit.WEEK -> TimeUnit.WEEK
                        DashboardTimeUnit.CUSTOM -> TimeUnit.WEEK
                    },
                    startTime = when (_uiState.value.selectedTimeUnit) {
                        DashboardTimeUnit.HOUR -> null
                        DashboardTimeUnit.DAY -> null
                        DashboardTimeUnit.WEEK -> null
                        DashboardTimeUnit.CUSTOM -> _uiState.value.selectedTimeRange.custom.start
                    },
                    endTime = when (_uiState.value.selectedTimeUnit) {
                        DashboardTimeUnit.HOUR -> _uiState.value.selectedTimeRange.hour.end
                        DashboardTimeUnit.DAY -> _uiState.value.selectedTimeRange.day.value
                        DashboardTimeUnit.WEEK -> _uiState.value.selectedTimeRange.week.end
                        DashboardTimeUnit.CUSTOM -> _uiState.value.selectedTimeRange.custom.end
                    }
                )
            }
                .flowOn(Dispatchers.IO)
                .collect {
                    it.onLeft { error ->
                        Logger.e(TAG) { "Error in auto refresh: $error" }
                        _uiState.update { state -> state.copy(errorCount = _uiState.value.errorCount + 1) }
                    }
                    it.onRight { value ->
                        _charts.value = value
                        _uiState.update { state -> state.copy(callCount = _uiState.value.callCount + 1) }
                    }
                    _uiState.update { state ->
                        state.copy(
                            isDataLoaded = true,
                            isRefreshing = false
                        )
                    }
                }
        }
    }

    fun stopAutoRefresh() {
        Logger.d(TAG) { "stopAutoRefresh" }
        autoRefreshJob?.cancel()
    }

    fun singleRefresh() {
        viewModelScope.launch {
            Logger.d(TAG) { "Single refresh ${_uiState.value.selectedTimeUnit}" }
            _uiState.update {
                it.copy(
                    selectedTimeRange = it.selectedTimeRange.withUpdatedRange(),
                    isRefreshing = true
                )
            }
            fetchTimeSeriesUseCase.singleFetch(
                FetchParameters(
                    timeUnit = when (_uiState.value.selectedTimeUnit) {
                        DashboardTimeUnit.HOUR -> TimeUnit.HOUR
                        DashboardTimeUnit.DAY -> TimeUnit.DAY
                        DashboardTimeUnit.WEEK -> TimeUnit.WEEK
                        DashboardTimeUnit.CUSTOM -> TimeUnit.WEEK
                    },
                    startTime = when (_uiState.value.selectedTimeUnit) {
                        DashboardTimeUnit.HOUR -> null
                        DashboardTimeUnit.DAY -> null
                        DashboardTimeUnit.WEEK -> null
                        DashboardTimeUnit.CUSTOM -> _uiState.value.selectedTimeRange.custom.start
                    },
                    endTime = when (_uiState.value.selectedTimeUnit) {
                        DashboardTimeUnit.HOUR -> _uiState.value.selectedTimeRange.hour.end
                        DashboardTimeUnit.DAY -> _uiState.value.selectedTimeRange.day.value
                        DashboardTimeUnit.WEEK -> _uiState.value.selectedTimeRange.week.end
                        DashboardTimeUnit.CUSTOM -> _uiState.value.selectedTimeRange.custom.end
                    }
                )
            ).onRight {
                _charts.value = it
                _uiState.update { state ->
                    state.copy(
                        isRefreshing = false,
                        callCount = _uiState.value.callCount + 1
                    )
                }
            }
        }
    }

    fun onTimeUnitSelected(timeUnit: DashboardTimeUnit) {
        _uiState.value = _uiState.value.copy(selectedTimeUnit = timeUnit)
        _charts.value = listOf()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            dataRepository.saveDashboardSelectedTimeUnitIndex(timeUnit.ordinal)
            stopAutoRefresh()
            startAutoRefresh()
        }
    }

    fun onTimeSelected(range: SelectedTimeRange) {
        _uiState.update { it.copy(selectedTimeRange = range) }
        Logger.d(TAG) { "onTimeSelected range $range" }
        singleRefresh()
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }

    fun dragRange(dragDirection: RangeSelectionButton?) {
        when (dragDirection) {
            RangeSelectionButton.NEXT -> {
                when (_uiState.value.selectedTimeUnit) {
                    DashboardTimeUnit.HOUR -> _uiState.update {
                        it.copy(
                            selectedTimeRange = it.selectedTimeRange.withUpdatedHourRange(
                                it.selectedTimeRange.hour.selectedValue - 1
                            )
                        )
                    }

                    DashboardTimeUnit.DAY -> _uiState.update {
                        it.copy(
                            selectedTimeRange = it.selectedTimeRange.withUpdatedDayRange(
                                it.selectedTimeRange.day.selectedValue - 1
                            )
                        )
                    }

                    DashboardTimeUnit.WEEK -> _uiState.update {
                        it.copy(
                            selectedTimeRange = it.selectedTimeRange.withUpdatedWeekRange(
                                it.selectedTimeRange.week.selectedValue - 1
                            )
                        )
                    }

                    DashboardTimeUnit.CUSTOM -> {}
                }
            }

            RangeSelectionButton.PREV -> {
                when (_uiState.value.selectedTimeUnit) {
                    DashboardTimeUnit.HOUR -> _uiState.update {
                        it.copy(
                            selectedTimeRange = it.selectedTimeRange.withUpdatedHourRange(
                                it.selectedTimeRange.hour.selectedValue + 1
                            )
                        )
                    }

                    DashboardTimeUnit.DAY -> _uiState.update {
                        it.copy(
                            selectedTimeRange = it.selectedTimeRange.withUpdatedDayRange(
                                it.selectedTimeRange.day.selectedValue + 1
                            )
                        )
                    }

                    DashboardTimeUnit.WEEK -> _uiState.update {
                        it.copy(
                            selectedTimeRange = it.selectedTimeRange.withUpdatedWeekRange(
                                it.selectedTimeRange.week.selectedValue + 1
                            )
                        )
                    }

                    DashboardTimeUnit.CUSTOM -> {}
                }
            }

            else -> {}
        }
        Logger.d(TAG) { "dragRange $dragDirection ${_uiState.value.selectedTimeRange}" }
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}