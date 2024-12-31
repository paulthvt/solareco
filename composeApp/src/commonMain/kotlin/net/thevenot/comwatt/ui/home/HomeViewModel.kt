package net.thevenot.comwatt.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.plus
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.client.Session

class HomeViewModel(private val session: Session, private val client: ComwattApi) : ViewModel() {
    private var autoRefreshJob: Job? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _production = MutableStateFlow("")
    val production: StateFlow<String> = _production

    private val _consumption = MutableStateFlow("")
    val consumption: StateFlow<String> = _consumption

    private val _injection = MutableStateFlow("")
    val injection: StateFlow<String> = _injection

    private val _withdrawals = MutableStateFlow("")
    val withdrawals: StateFlow<String> = _withdrawals

    private val _updateDate = MutableStateFlow("")
    val updateDate: StateFlow<String> = _updateDate

    private val _lastRefreshDate = MutableStateFlow("")
    val lastRefreshDate: StateFlow<String> = _lastRefreshDate

    fun load() {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                _isLoading.value = true
                val sites = client.sites(session.token)
                sites.first().id.let {
                    val fetchSiteTimeSeries = client.fetchSiteTimeSeries(session.token, it)
                    println("Sites: $fetchSiteTimeSeries")
                    _production.value = fetchSiteTimeSeries.productions.last().toString()
                    _consumption.value = fetchSiteTimeSeries.consumptions.last().toString()
                    _injection.value = fetchSiteTimeSeries.injections.last().toString()
                    _withdrawals.value = fetchSiteTimeSeries.withdrawals.last().toString()

                    val lastUpdateTimestamp = Instant.parse(fetchSiteTimeSeries.timestamps.last().toString())
                    _updateDate.value = lastUpdateTimestamp.format(DateTimeComponents.Formats.RFC_1123)
                    _lastRefreshDate.value = Clock.System.now().format(DateTimeComponents.Formats.RFC_1123)

                    val nextUpdateTimestamp = lastUpdateTimestamp.plus(2, DateTimeUnit.MINUTE)
                    val delayMillis = nextUpdateTimestamp.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()

                    _isLoading.value = false
                    delay(delayMillis)
                }
            }
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}

