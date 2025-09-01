package net.thevenot.comwatt.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchParameters
import net.thevenot.comwatt.domain.FetchTimeSeriesUseCase
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.ChartTimeSeries
import net.thevenot.comwatt.domain.model.TimeUnit
import net.thevenot.comwatt.domain.utils.computeSiteStats
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.AggregationType
import net.thevenot.comwatt.model.type.MeasureKind
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
            loadSelectedTimeUnit()
            launch {
                updateTimeRangeAndFetchData()
            }
            launch {
                refreshRangeStats()
            }
        }
    }

    private suspend fun loadSelectedTimeUnit() {
        val selectedTimeUnitIndex =
            dataRepository.getSettings().firstOrNull()?.dashboardSelectedTimeUnitIndex
        Logger.d(TAG) { "startAutoRefresh selectedTimeUnitIndex $selectedTimeUnitIndex" }

        selectedTimeUnitIndex?.let { index ->
            val timeUnit = DashboardTimeUnit.entries.getOrNull(index) ?: DashboardTimeUnit.HOUR
            _uiState.update { it.copy(selectedTimeUnit = timeUnit) }
            Logger.d(TAG) { "startAutoRefresh selectedTimeUnit ${_uiState.value.selectedTimeUnit}" }
        }
    }

    private suspend fun updateTimeRangeAndFetchData() {
        fetchTimeSeriesUseCase.invoke {
            _uiState.update {
                it.copy(selectedTimeRange = it.selectedTimeRange.withUpdatedHourRange())
            }
            Logger.d(TAG) { "startAutoRefresh selectedTimeRange ${_uiState.value.selectedTimeRange}" }
            createFetchParameters()
        }
            .flowOn(Dispatchers.IO)
            .collect { result ->
                handleFetchResult(result)
            }
    }

    private fun handleFetchResult(result: Either<DomainError, List<ChartTimeSeries>>) {
        result.onLeft { error ->
            Logger.e(TAG) { "Error in auto refresh: $error" }
            _uiState.update { state -> state.copy(errorCount = _uiState.value.errorCount + 1) }
        }

        result.onRight { value ->
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

            launch {
                fetchTimeSeriesUseCase.singleFetch(createFetchParameters())
                    .onRight {
                        _charts.value = it
                        _uiState.update { state ->
                            state.copy(
                                isRefreshing = false,
                                callCount = _uiState.value.callCount + 1
                            )
                        }
                    }
            }
            launch {
                refreshRangeStats()
            }
        }
    }

    private fun createFetchParameters(): FetchParameters {
        val currentState = _uiState.value

        return FetchParameters(
            timeUnit = mapDashboardTimeUnitToTimeUnit(currentState.selectedTimeUnit),
            startTime = getStartTime(currentState.selectedTimeUnit, currentState.selectedTimeRange),
            endTime = getEndTime(currentState.selectedTimeUnit, currentState.selectedTimeRange)
        )
    }

    private fun mapDashboardTimeUnitToTimeUnit(unit: DashboardTimeUnit): TimeUnit {
        return when (unit) {
            DashboardTimeUnit.HOUR -> TimeUnit.HOUR
            DashboardTimeUnit.DAY -> TimeUnit.DAY
            DashboardTimeUnit.WEEK -> TimeUnit.WEEK
            DashboardTimeUnit.CUSTOM -> TimeUnit.CUSTOM
        }
    }

    private fun getStartTime(timeUnit: DashboardTimeUnit, timeRange: SelectedTimeRange): Instant? {
        return when (timeUnit) {
            DashboardTimeUnit.HOUR,
            DashboardTimeUnit.DAY,
            DashboardTimeUnit.WEEK -> null
            DashboardTimeUnit.CUSTOM -> timeRange.custom.start
        }
    }

    private fun getEndTime(timeUnit: DashboardTimeUnit, timeRange: SelectedTimeRange): Instant {
        return when (timeUnit) {
            DashboardTimeUnit.HOUR -> timeRange.hour.end
            DashboardTimeUnit.DAY -> timeRange.day.value
            DashboardTimeUnit.WEEK -> timeRange.week.end
            DashboardTimeUnit.CUSTOM -> timeRange.custom.end
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

            null -> {}
        }
        Logger.d(TAG) { "dragRange $dragDirection ${_uiState.value.selectedTimeRange}" }
    }

    private suspend fun refreshRangeStats() {
        val settings = dataRepository.getSettings().firstOrNull() ?: return
        val siteId = settings.siteId ?: return
        val (start, end) = getRangeBounds(
            _uiState.value.selectedTimeUnit,
            _uiState.value.selectedTimeRange
        )

        val result = dataRepository.api.fetchSiteTimeSeries(
            siteId = siteId,
            startTime = start,
            endTime = end,
            measureKind = MeasureKind.QUANTITY,
            aggregationLevel = AggregationLevel.NONE,
            aggregationType = AggregationType.SUM
        )
        result.onRight { ts ->
            val lastTs = ts.timestamps.lastOrNull()?.let { Instant.parse(it) }
            val stats = computeSiteStats(
                productions = ts.productions,
                consumptions = ts.consumptions,
                injections = ts.injections,
                withdrawals = ts.withdrawals,
                lastTimestamp = lastTs
            )
            _uiState.update { it.copy(rangeStats = stats) }
        }
    }

    private fun getRangeBounds(
        unit: DashboardTimeUnit,
        range: SelectedTimeRange
    ): Pair<Instant, Instant> {
        return when (unit) {
            DashboardTimeUnit.HOUR -> range.hour.start to range.hour.end
            DashboardTimeUnit.DAY -> {
                val tz = TimeZone.currentSystemDefault()
                val date = range.day.value.toLocalDateTime(tz).date
                val start = date.atStartOfDayIn(tz)
                val end = start.plus(1, DateTimeUnit.DAY, tz)
                    .minus(1, DateTimeUnit.NANOSECOND)
                start to end
            }

            DashboardTimeUnit.WEEK -> range.week.start to range.week.end
            DashboardTimeUnit.CUSTOM -> range.custom.start to range.custom.end
        }
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}