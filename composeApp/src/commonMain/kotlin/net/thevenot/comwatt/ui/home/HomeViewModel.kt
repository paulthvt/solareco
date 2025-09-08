package net.thevenot.comwatt.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.jamesyox.kastro.common.HorizonState
import dev.jamesyox.kastro.sol.calculateSolarState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.thevenot.comwatt.domain.FetchCurrentSiteUseCase
import net.thevenot.comwatt.domain.FetchSiteDailyDataUseCase
import net.thevenot.comwatt.domain.FetchSiteRealtimeDataUseCase
import net.thevenot.comwatt.domain.FetchWeatherUseCase


class HomeViewModel(
    private val fetchSiteRealtimeDataUseCase: FetchSiteRealtimeDataUseCase,
    private val fetchSiteDailyDataUseCase: FetchSiteDailyDataUseCase,
    private val fetchWeatherUseCase: FetchWeatherUseCase,
    private val fetchCurrentSiteUseCase: FetchCurrentSiteUseCase
) : ViewModel() {
    private var autoRefreshJob: Job? = null

    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> get() = _uiState

    fun enableProductionGauge(enabled: Boolean) {
        _uiState.update { it.copy(productionGaugeEnabled = enabled) }
    }

    fun enableConsumptionGauge(enabled: Boolean) {
        _uiState.update { it.copy(consumptionGaugeEnabled = enabled) }
    }

    fun enableInjectionGauge(enabled: Boolean) {
        _uiState.update { it.copy(injectionGaugeEnabled = enabled) }
    }

    fun enableWithdrawalsGauge(enabled: Boolean) {
        _uiState.update { it.copy(withdrawalsGaugeEnabled = enabled) }
    }

    fun startAutoRefresh() {
        Logger.d(TAG) { "startAutoRefresh ${this@HomeViewModel}" }
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            launch {
                fetchCurrentSiteUseCase.invoke().onRight { site ->
                    _uiState.update { state ->
                        state.copy(siteName = site?.name)
                    }
                }.onLeft { error ->
                    Logger.e(TAG) { "Error fetching current site: $error" }
                }
            }
            launch {
                fetchSiteRealtimeDataUseCase.invoke().flowOn(Dispatchers.IO).catch {
                    Logger.e(TAG) { "Error in auto refresh: $it" }
                    _uiState.update { state ->
                        state.copy(
                            errorCount = _uiState.value.errorCount + 1,
                            lastErrorMessage = it.message ?: "Unknown error"
                        )
                    }
                }.collect { result ->
                    result.onLeft { error ->
                        Logger.e(TAG) { "Error in auto refresh: $error" }
                        _uiState.update { state ->
                            state.copy(errorCount = _uiState.value.errorCount + 1)
                        }
                    }
                    result.onRight { value ->
                        _uiState.update { state ->
                            state.copy(
                                siteRealtimeData = value,
                                callCount = _uiState.value.callCount + 1,
                                lastRefreshInstant = value.lastUpdateTimestamp
                            )
                        }
                        updateTimeDifference()
                    }
                    _uiState.update { state -> state.copy(isDataLoaded = true) }
                }
            }
            launch {
                fetchSiteDailyDataUseCase.invoke().flowOn(Dispatchers.IO).catch {
                    Logger.e(TAG) { "Error in daily data auto refresh: $it" }
                    _uiState.update { state ->
                        state.copy(
                            errorCount = _uiState.value.errorCount + 1,
                            lastErrorMessage = it.message ?: "Unknown error"
                        )
                    }
                }.collect { result ->
                    result.onLeft { error ->
                        Logger.e(TAG) { "Error fetching daily data: $error" }
                        _uiState.update { state ->
                            state.copy(errorCount = _uiState.value.errorCount + 1)
                        }
                    }
                    result.onRight { dailyData ->
                        Logger.d(TAG) { "Daily data: $dailyData" }
                        _uiState.update { state ->
                            state.copy(siteDailyData = dailyData)
                        }
                    }
                }
            }
            launch {
                fetchWeatherUseCase.invoke().flowOn(Dispatchers.IO).catch {
                    Logger.e(TAG) { "Error in auto refresh: $it" }
                    _uiState.update { state ->
                        state.copy(
                            errorCount = _uiState.value.errorCount + 1,
                            lastErrorMessage = it.message ?: "Unknown error"
                        )
                    }
                }.collect {
                    it.onLeft { error ->
                        Logger.e(TAG) { "Error fetching weather: $error" }
                        _uiState.update { state ->
                            state.copy(
                                errorCount = _uiState.value.errorCount + 1
                            )
                        }
                    }
                    it.onRight { weather ->
                        _uiState.update { state ->
                            state.copy(weatherForecast = weather)
                        }
                        updateSunState(weather.latitude, weather.longitude)
                    }
                }
            }
        }
    }

    fun singleRefresh() {
        viewModelScope.launch {
            Logger.d(TAG) { "Single refresh ${this@HomeViewModel}" }
            _uiState.update { it.copy(isRefreshing = true) }
            val siteRealtimeDeferred = async { fetchSiteRealtimeDataUseCase.singleFetch() }
            val siteDailyDeferred = async { fetchSiteDailyDataUseCase.singleFetch() }
            val weatherDeferred = async { fetchWeatherUseCase.singleFetch() }

            val siteRealtimeResult = siteRealtimeDeferred.await()
            siteRealtimeResult.onRight {
                _uiState.update { state ->
                    state.copy(
                        callCount = _uiState.value.callCount + 1,
                        lastRefreshInstant = it.lastUpdateTimestamp
                    )
                }
                updateTimeDifference()
            }

            val siteDailyResult = siteDailyDeferred.await()
            siteDailyResult.onRight { dailyData ->
                _uiState.update { state ->
                    state.copy(siteDailyData = dailyData)
                }
            }

            val weatherResult = weatherDeferred.await()
            weatherResult.onRight {
                _uiState.update { state ->
                    state.copy(weatherForecast = it)
                }
                updateSunState(it.latitude, it.longitude)
            }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun updateTimeDifference() {
        _uiState.value.lastRefreshInstant?.let { lastRefreshInstant ->
            val now = Clock.System.now()
            val minutesDifference = (now - lastRefreshInstant).inWholeMinutes.toInt()
            _uiState.update { it.copy(timeDifference = minutesDifference) }
        }
    }

    fun updateSunState(lat: Double, lon: Double) {
        val sunState = Clock.System.now().calculateSolarState(
            latitude = lat,
            longitude = lon,
        )
        _uiState.update { it.copy(isDay = sunState.horizonState == HorizonState.Up) }
    }

    fun stopAutoRefresh() {
        Logger.d(TAG) { "stopAutoRefresh" }
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
