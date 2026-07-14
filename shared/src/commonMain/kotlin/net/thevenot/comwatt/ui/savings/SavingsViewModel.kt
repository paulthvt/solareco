package net.thevenot.comwatt.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchCurrentSiteUseCase
import net.thevenot.comwatt.domain.savings.ComputeSavingsUseCase
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.ui.dashboard.RangeSelectionButton
import net.thevenot.comwatt.ui.dashboard.SelectedTimeRange
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit

class SavingsViewModel(
    private val computeSavingsUseCase: ComputeSavingsUseCase,
    private val siteIdProvider: suspend () -> Int?,
    private val settingsProvider: suspend () -> TariffConfig,
    private val initialTimeUnitProvider: suspend () -> DashboardTimeUnit? = { null },
    private val persistTimeUnit: suspend (Int) -> Unit = {},
) : ViewModel() {
    constructor(dataRepository: DataRepository) : this(
        computeSavingsUseCase = ComputeSavingsUseCase(dataRepository),
        siteIdProvider = {
            when (val r = FetchCurrentSiteUseCase(dataRepository).invoke()) {
                is Either.Right -> r.value?.id
                is Either.Left -> null
            }
        },
        settingsProvider = {
            dataRepository.getSettings().first().tariffConfigJson
                ?.let { TariffConfig.decode(it) } ?: TariffConfig.defaults()
        },
        initialTimeUnitProvider = {
            dataRepository.getSettings().first().savingsSelectedTimeUnitIndex
                ?.let { DashboardTimeUnit.entries.getOrNull(it) }
        },
        persistTimeUnit = { idx -> dataRepository.saveSavingsSelectedTimeUnitIndex(idx) },
    )

    private val _uiState = MutableStateFlow(SavingsScreenState())
    val uiState: StateFlow<SavingsScreenState> get() = _uiState

    init {
        viewModelScope.launch {
            initialTimeUnitProvider()?.let { unit ->
                _uiState.update { it.copy(selectedTimeUnit = unit) }
            }
        }
        refresh()
    }

    fun onTimeUnitSelected(unit: DashboardTimeUnit) {
        _uiState.update { it.copy(selectedTimeUnit = unit) }
        viewModelScope.launch {
            persistTimeUnit(unit.ordinal)
        }
        refresh()
    }

    fun dragRange(direction: RangeSelectionButton) {
        _uiState.update { st ->
            val r = st.selectedTimeRange
            val step = if (direction == RangeSelectionButton.NEXT) -1 else 1
            val updated = when (st.selectedTimeUnit) {
                DashboardTimeUnit.HOUR -> r.withUpdatedHourRange(r.hour.selectedValue + step)
                DashboardTimeUnit.SIXHOUR -> r.withUpdatedSixHourRange(r.sixHour.selectedValue + step)
                DashboardTimeUnit.DAY -> r.withUpdatedDayRange(r.day.selectedValue + step)
                DashboardTimeUnit.WEEK -> r.withUpdatedWeekRange(r.week.selectedValue + step)
                DashboardTimeUnit.CUSTOM -> r
            }
            st.copy(selectedTimeRange = updated)
        }
        refresh()
    }

    fun onTimeSelected(range: SelectedTimeRange) {
        _uiState.update { it.copy(selectedTimeRange = range) }
        refresh()
    }

    /** Pull-to-refresh: keep existing content visible via [SavingsScreenState.isRefreshing]
     *  instead of the full-screen [SavingsScreenState.isLoading] indicator. */
    fun pullToRefresh() = refresh(isPullToRefresh = true)

    fun refresh(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isPullToRefresh) it.copy(isRefreshing = true, hasError = false)
                else it.copy(isLoading = true, hasError = false)
            }
            val config = settingsProvider()
            val siteId = siteIdProvider()
            if (siteId == null) { _uiState.update { it.copy(isLoading = false, isRefreshing = false, hasError = true, config = config, configConfirmed = config.confirmedByUser) }; return@launch }
            val zone = TimeZone.currentSystemDefault()
            val state = _uiState.value
            val range = state.selectedTimeRange.withUpdatedRange() // refresh against "now"
            val (startLdt, endLdt) = bounds(state.selectedTimeUnit, range)
            val result = computeSavingsUseCase(
                siteId = siteId, start = startLdt.toInstant(zone), end = endLdt.toInstant(zone),
                config = config, zone = zone,
            )
            _uiState.update {
                val base = it.copy(isLoading = false, isRefreshing = false, selectedTimeRange = range, config = config, configConfirmed = config.confirmedByUser)
                when (result) {
                    is Either.Right -> base.copy(hasError = false, breakdown = result.value)
                    // On pull-to-refresh, keep the existing content visible instead of
                    // swapping to the full error view; only initial load shows the error state.
                    is Either.Left -> base.copy(hasError = !isPullToRefresh)
                }
            }
        }
    }

    private fun bounds(unit: DashboardTimeUnit, r: SelectedTimeRange) = when (unit) {
        DashboardTimeUnit.HOUR -> r.hour.start to r.hour.end
        DashboardTimeUnit.SIXHOUR -> r.sixHour.start to r.sixHour.end
        DashboardTimeUnit.DAY -> r.day.start to r.day.end
        DashboardTimeUnit.WEEK -> r.week.start to r.week.end
        DashboardTimeUnit.CUSTOM -> r.custom.start to r.custom.end
    }
}
