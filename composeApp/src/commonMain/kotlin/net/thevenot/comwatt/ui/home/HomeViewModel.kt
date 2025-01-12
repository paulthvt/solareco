package net.thevenot.comwatt.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either.Left
import arrow.core.Either.Right
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.plus
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.client.Session
import net.thevenot.comwatt.model.ApiError


class HomeViewModel(
    private val session: Session,
    private val dataRepository: DataRepository
) : ViewModel() {
    private var autoRefreshJob: Job? = null
    private val mutex = Mutex()

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
            val siteId = dataRepository.getSettings().firstOrNull()?.siteId
            siteId?.let { id ->
                Napier.d(tag = TAG) { "Site id: $id" }
                while (isActive) {
                    mutex.withLock {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                        Napier.d(tag = TAG) { "calling site time series" }
                        _uiState.value = _uiState.value.copy(callCount = _uiState.value.callCount + 1)
                        val response = dataRepository.api.fetchSiteTimeSeries(session.token, id)
                        when (response) {
                            is Left -> {
                                Napier.d(tag = TAG) { "error: ${response.value}" }

                                _uiState.value = _uiState.value.copy(
                                    errorCount = _uiState.value.errorCount + 1,
                                    lastErrorMessage = when (val value = response.value) {
                                        is ApiError.HttpError -> {
                                            value.errorBody
                                        }

                                        is ApiError.SerializationError -> {
                                            value.message
                                        }

                                        is ApiError.GenericError -> {
                                            value.message
                                        }
                                    } ?: "Unknown error"
                                )
                                delay(FALLBACK_DELAY)
                            }

                            is Right -> {
                                val lastUpdateTimestamp =
                                    Instant.parse(response.value.timestamps.last().toString())

                                _uiState.value = _uiState.value.copy(
                                    production = response.value.productions.last(),
                                    consumption = response.value.consumptions.last(),
                                    injection = response.value.injections.last(),
                                    withdrawals = response.value.withdrawals.last(),
                                    consumptionRate = response.value.consumptions.last() / MAX_POWER,
                                    productionRate = response.value.productions.last() / MAX_POWER,
                                    injectionRate = response.value.injections.last() / MAX_POWER,
                                    withdrawalsRate = response.value.withdrawals.last() / MAX_POWER,
                                    updateDate = lastUpdateTimestamp.format(DateTimeComponents.Formats.RFC_1123),
                                    lastRefreshDate = Clock.System.now().format(DateTimeComponents.Formats.RFC_1123),
                                    isLoading = false
                                )

                                val delayMillis = computeDelay(lastUpdateTimestamp)
                                Napier.d(tag = TAG) { "waiting for $delayMillis milliseconds" }
                                delay(delayMillis)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun computeDelay(lastUpdateTimestamp: Instant): Long {
        val nextUpdateTimestamp = lastUpdateTimestamp.plus(2, DateTimeUnit.MINUTE)
        val delayMillis = (nextUpdateTimestamp.toEpochMilliseconds() - Clock.System.now()
            .toEpochMilliseconds()).coerceAtLeast(0)
        if (delayMillis == 0L) {
            return FALLBACK_DELAY
        }
        return delayMillis
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
        private const val FALLBACK_DELAY = 10_000L
        const val MAX_POWER = 7000.0
        private const val TAG = "HomeViewModel"
    }
}

