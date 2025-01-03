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


class HomeViewModel(
    private val session: Session,
    private val dataRepository: DataRepository
) : ViewModel() {
    private var autoRefreshJob: Job? = null
    private val mutex = Mutex()
    private val _callNumber = MutableStateFlow(0)
    val callNumber: StateFlow<Int> get() = _callNumber

    private val _errorNumber = MutableStateFlow(0)
    val errorNumber: StateFlow<Int> get() = _errorNumber

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
        Napier.d(tag = TAG) { "startAutoRefresh ${this@HomeViewModel}" }
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            val siteId = dataRepository.getSettings().firstOrNull()?.siteId
            siteId?.let { id ->
                Napier.d(tag = TAG) { "Site id: $id" }
                while (isActive) {
                    mutex.withLock {
                        _isLoading.value = true
                        Napier.d(tag = TAG) { "calling site time series" }
                        _callNumber.value++
                        val response = dataRepository.api.fetchSiteTimeSeries(session.token, id)
                        when (response) {
                            is Left -> {
                                Napier.d(tag = TAG) { "error: ${response.value}" }
                                _errorNumber.value++
                                delay(FALLBACK_DELAY)
                            }

                            is Right -> {
                                _production.value = response.value.productions.last().toString()
                                _consumption.value = response.value.consumptions.last().toString()
                                _injection.value = response.value.injections.last().toString()
                                _withdrawals.value = response.value.withdrawals.last().toString()

                                val lastUpdateTimestamp =
                                    Instant.parse(response.value.timestamps.last().toString())
                                _updateDate.value =
                                    lastUpdateTimestamp.format(DateTimeComponents.Formats.RFC_1123)
                                _lastRefreshDate.value =
                                    Clock.System.now().format(DateTimeComponents.Formats.RFC_1123)

                                val delayMillis = computeDelay(lastUpdateTimestamp)

                                _isLoading.value = false
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
        private const val TAG = "HomeViewModel"
    }
}

