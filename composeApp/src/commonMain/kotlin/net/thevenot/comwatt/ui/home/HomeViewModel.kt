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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchCurrentSiteUseCase
import net.thevenot.comwatt.domain.FetchSiteDailyDataUseCase
import net.thevenot.comwatt.domain.FetchSiteRealtimeDataUseCase
import net.thevenot.comwatt.domain.FetchWeatherUseCase
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.ui.settings.SettingsViewModel.Companion.DEFAULT_MAX_POWER_GAUGE
import kotlin.time.Clock


class HomeViewModel(
    dataRepository: DataRepository,
    private val fetchSiteRealtimeDataUseCase: FetchSiteRealtimeDataUseCase,
    private val fetchSiteDailyDataUseCase: FetchSiteDailyDataUseCase,
    private val fetchWeatherUseCase: FetchWeatherUseCase,
    private val fetchCurrentSiteUseCase: FetchCurrentSiteUseCase
) : ViewModel() {
    private var autoRefreshJob: Job? = null

    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> get() = _uiState

    init {
        dataRepository.getSettings()
            .onEach { settings ->
                _uiState.update {
                    it.copy(
                        powerMaxGauge = (settings.maxPowerGauge ?: DEFAULT_MAX_POWER_GAUGE) * 1000
                    )
                }
            }
            .launchIn(viewModelScope)
    }

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
        _uiState.update { it.copy(lastErrorMessage = "") }
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
                    handleException("Exception in auto refresh", it)
                }.collect { result ->
                    result.onLeft { handleError("Error in real time data auto refresh", it) }
                        .onRight { value ->
                            _uiState.update { state ->
                                state.copy(
                                    siteRealtimeData = value,
                                    callCount = _uiState.value.callCount + 1,
                                    lastRefreshInstant = value.lastUpdateTimestamp
                                )
                            }
                            updateTimeDifference()
                            _uiState.update { state -> state.copy(isDataLoaded = true) }
                        }
                }
            }
            launch {
                fetchSiteDailyDataUseCase.invoke().flowOn(Dispatchers.IO).catch {
                    handleException("Error in daily data auto refresh", it)
                }.collect { result ->
                    result.onLeft { error ->
                        Logger.e(TAG) { "Error fetching daily data: $error" }
                        _uiState.update { state ->
                            state.copy(
                                lastErrorMessage = when (error) {
                                    is DomainError.Api -> error.error.toString()
                                    is DomainError.Generic -> error.message
                                }
                            )
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
                    handleException("Error in auto refresh", it)
                }.collect {
                    it.onLeft { error ->
                        Logger.e(TAG) { "Error fetching weather: $error" }
                        _uiState.update { state ->
                            state.copy(
                                lastErrorMessage = when (error) {
                                    is DomainError.Api -> error.error.toString()
                                    is DomainError.Generic -> error.message
                                }
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
            _uiState.update { it.copy(isRefreshing = true, lastErrorMessage = "") }
            val siteRealtimeDeferred = async { fetchSiteRealtimeDataUseCase.singleFetch() }
            val siteDailyDeferred = async { fetchSiteDailyDataUseCase.singleFetch() }
            val weatherDeferred = async { fetchWeatherUseCase.singleFetch() }

            val siteRealtimeResult = siteRealtimeDeferred.await()
            siteRealtimeResult.onLeft {
                handleError("Error fetching real time data", it)
            }.onRight {
                _uiState.update { state ->
                    state.copy(
                        callCount = _uiState.value.callCount + 1,
                        lastRefreshInstant = it.lastUpdateTimestamp
                    )
                }
                updateTimeDifference()
            }

            val siteDailyResult = siteDailyDeferred.await()
            siteDailyResult.onLeft {
                handleError("Error fetching daily data", it)
            }.onRight { dailyData ->
                _uiState.update { state ->
                    state.copy(siteDailyData = dailyData)
                }
            }

            val weatherResult = weatherDeferred.await()
            weatherResult.onLeft {
                handleError("Error fetching daily weather", it)
            }.onRight {
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
        private const val TAG = "HomeViewModel"
    }
}
