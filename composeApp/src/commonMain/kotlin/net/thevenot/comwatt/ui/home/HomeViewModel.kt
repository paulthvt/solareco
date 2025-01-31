package net.thevenot.comwatt.ui.home

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
import net.thevenot.comwatt.domain.FetchSiteTimeSeriesUseCase


class HomeViewModel(
    private val fetchSiteTimeSeriesUseCase: FetchSiteTimeSeriesUseCase
) : ViewModel() {
    private var autoRefreshJob: Job? = null

    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> get() = _uiState

    fun enableProductionGauge(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(productionGaugeEnabled = enabled)
    }

    fun enableConsumptionGauge(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(consumptionGaugeEnabled = enabled)
    }

    fun enableInjectionGauge(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(injectionGaugeEnabled = enabled)
    }

    fun enableWithdrawalsGauge(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(withdrawalsGaugeEnabled = enabled)
    }

    fun load() {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        Napier.d(tag = TAG) { "startAutoRefresh ${this@HomeViewModel}" }
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            fetchSiteTimeSeriesUseCase.invoke()
                .flowOn(Dispatchers.IO)
                .catch {
                    Napier.e(tag = TAG) { "Error in auto refresh: $it" }
                    _uiState.value = _uiState.value.copy(
                        errorCount = _uiState.value.errorCount + 1,
                        lastErrorMessage = it.message ?: "Unknown error"
                    )
                }
                .collect {
                    _uiState.value = _uiState.value.copy(
                        production = it.production,
                        consumption = it.consumption,
                        injection = it.injection,
                        withdrawals = it.withdrawals,
                        consumptionRate = it.consumptionRate,
                        productionRate = it.productionRate,
                        injectionRate = it.injectionRate,
                        withdrawalsRate = it.withdrawalsRate,
                        updateDate = it.updateDate,
                        lastRefreshDate = it.lastRefreshDate,
                        callCount = _uiState.value.callCount + 1
                    )
                }
        }
    }

    fun singleRefresh() {
        viewModelScope.launch {
            Napier.d(tag = TAG) { "Single refresh ${this@HomeViewModel}" }
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            fetchSiteTimeSeriesUseCase.singleFetch().onRight {
                _uiState.value = _uiState.value.copy(
                    production = it.production,
                    consumption = it.consumption,
                    injection = it.injection,
                    withdrawals = it.withdrawals,
                    consumptionRate = it.consumptionRate,
                    productionRate = it.productionRate,
                    injectionRate = it.injectionRate,
                    withdrawalsRate = it.withdrawalsRate,
                    updateDate = it.updateDate,
                    lastRefreshDate = it.lastRefreshDate,
                    isRefreshing = false,
                    callCount = _uiState.value.callCount + 1
                )
            }
        }
    }

    private fun stopAutoRefresh() {
        Napier.d(tag = TAG) { "stopAutoRefresh" }
        autoRefreshJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }

    companion object {
        const val MAX_POWER = 9000.0
        private const val TAG = "HomeViewModel"
    }
}

