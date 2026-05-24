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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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
import net.thevenot.comwatt.ui.settings.SettingsViewModel.Companion.DEFAULT_PRODUCTION_NOISE_THRESHOLD
import kotlin.time.Instant

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
        _uiState.update { it.copy(lastErrorMessage = "") }
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
                it.copy(selectedTimeRange = it.selectedTimeRange.withUpdatedRange())
            }
            Logger.d(TAG) { "startAutoRefresh selectedTimeRange ${_uiState.value.selectedTimeRange}" }
            createFetchParameters()
        }
            .flowOn(Dispatchers.IO).catch {
                handleException("Exception in auto refresh", it)
            }
            .collect { result ->
                handleFetchResult(result)
            }
    }

    private fun handleFetchResult(result: Either<DomainError, List<ChartTimeSeries>>) {
        result.onLeft { error ->
            handleError("Error in data auto refresh", error)
        }.onRight { value ->
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
                    isRefreshing = true,
                    lastErrorMessage = ""
                )
            }

            launch {
                fetchTimeSeriesUseCase.singleFetch(createFetchParameters())
                    .onLeft { error -> handleError("Error in data single refresh", error) }
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
        val tz = TimeZone.currentSystemDefault()
        return FetchParameters(
            timeUnit = mapDashboardTimeUnitToTimeUnit(currentState.selectedTimeUnit),
            startTime = getStartTime(
                currentState.selectedTimeUnit,
                currentState.selectedTimeRange
            )?.toInstant(tz),
            endTime = getEndTime(
                currentState.selectedTimeUnit,
                currentState.selectedTimeRange
            ).toInstant(tz)
        )
    }

    private fun mapDashboardTimeUnitToTimeUnit(unit: DashboardTimeUnit): TimeUnit {
        return when (unit) {
            DashboardTimeUnit.HOUR -> TimeUnit.HOUR
            DashboardTimeUnit.SIXHOUR -> TimeUnit.HOUR
            DashboardTimeUnit.DAY -> TimeUnit.DAY
            DashboardTimeUnit.WEEK -> TimeUnit.WEEK
            DashboardTimeUnit.CUSTOM -> TimeUnit.CUSTOM
        }
    }

    private fun getStartTime(
        timeUnit: DashboardTimeUnit,
        timeRange: SelectedTimeRange
    ): LocalDateTime? {
        return when (timeUnit) {
            DashboardTimeUnit.HOUR,
            DashboardTimeUnit.DAY,
            DashboardTimeUnit.WEEK -> null
            DashboardTimeUnit.SIXHOUR -> timeRange.sixHour.start
            DashboardTimeUnit.CUSTOM -> timeRange.custom.start
        }
    }

    private fun getEndTime(
        timeUnit: DashboardTimeUnit,
        timeRange: SelectedTimeRange
    ): LocalDateTime {
        return when (timeUnit) {
            DashboardTimeUnit.HOUR -> timeRange.hour.end
            DashboardTimeUnit.SIXHOUR -> timeRange.sixHour.end
            DashboardTimeUnit.DAY -> timeRange.day.end
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

                    DashboardTimeUnit.SIXHOUR -> _uiState.update {
                        it.copy(
                            selectedTimeRange = it.selectedTimeRange.withUpdatedSixHourRange(
                                it.selectedTimeRange.sixHour.selectedValue - 1
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

                    DashboardTimeUnit.SIXHOUR -> _uiState.update {
                        it.copy(
                            selectedTimeRange = it.selectedTimeRange.withUpdatedSixHourRange(
                                it.selectedTimeRange.sixHour.selectedValue + 1
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
            startTime = start.toInstant(TimeZone.currentSystemDefault()),
            endTime = end.toInstant(TimeZone.currentSystemDefault()),
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
                productionNoiseThreshold = settings.productionNoiseThreshold
                    ?: DEFAULT_PRODUCTION_NOISE_THRESHOLD,
                lastTimestamp = lastTs
            )
            _uiState.update { it.copy(rangeStats = stats) }
        }
    }

    private fun getRangeBounds(
        unit: DashboardTimeUnit,
        range: SelectedTimeRange
    ): Pair<LocalDateTime, LocalDateTime> {
        return when (unit) {
            DashboardTimeUnit.HOUR -> range.hour.start to range.hour.end
            DashboardTimeUnit.SIXHOUR -> range.sixHour.start to range.sixHour.end
            DashboardTimeUnit.DAY -> range.day.start to range.day.end
            DashboardTimeUnit.WEEK -> range.week.start to range.week.end
            DashboardTimeUnit.CUSTOM -> range.custom.start to range.custom.end
        }
    }

    fun toggleCardExpansion(chartName: String) {

        val isCurrentlyExpanded = _uiState.value.expandedCards.contains(chartName)

        if (isCurrentlyExpanded) {
            _uiState.update { it.copy(expandedCards = it.expandedCards - chartName) }
        } else {
            _uiState.update { it.copy(expandedCards = it.expandedCards + chartName) }
        }
    }

    private fun handleException(log: String, error: Throwable) {
        Logger.e(TAG) { "$log: $error" }
        _uiState.update { state ->
            state.copy(
                lastErrorMessage = error.message ?: "Unknown error"
            )
        }
    }

    private fun handleError(log: String, error: DomainError) {
        Logger.e(TAG) { "$log: $error" }
        _uiState.update { state ->
            state.copy(
                lastErrorMessage = when (error) {
                    is DomainError.Api -> error.error.toString()
                    is DomainError.Generic -> error.message
                }
            )
        }
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}