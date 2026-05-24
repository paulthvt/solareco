package net.thevenot.comwatt.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository

class SettingsViewModel(val dataRepository: DataRepository) : ViewModel() {

    companion object {
        const val DEFAULT_MAX_POWER_GAUGE = 9
        const val DEFAULT_PRODUCTION_NOISE_THRESHOLD = 5
    }

    private val _maxPowerGauge = MutableStateFlow(DEFAULT_MAX_POWER_GAUGE)
    val maxPowerGauge: StateFlow<Int> = _maxPowerGauge

    private val _productionNoiseThreshold = MutableStateFlow(DEFAULT_PRODUCTION_NOISE_THRESHOLD)
    val productionNoiseThreshold: StateFlow<Int> = _productionNoiseThreshold

    init {
        dataRepository.getSettings()
            .onEach { settings ->
                _maxPowerGauge.value = settings.maxPowerGauge ?: DEFAULT_MAX_POWER_GAUGE
                _productionNoiseThreshold.value =
                    settings.productionNoiseThreshold ?: DEFAULT_PRODUCTION_NOISE_THRESHOLD
            }
            .launchIn(viewModelScope)
    }

    fun updateMaxPowerGauge(maxPower: Int) {
        viewModelScope.launch {
            dataRepository.saveMaxPowerGauge(maxPower)
        }
    }

    fun updateProductionNoiseThreshold(threshold: Int) {
        viewModelScope.launch {
            dataRepository.saveProductionNoiseThreshold(threshold)
        }
    }
}
