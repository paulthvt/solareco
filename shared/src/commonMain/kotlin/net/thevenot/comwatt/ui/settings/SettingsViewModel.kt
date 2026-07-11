package net.thevenot.comwatt.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.model.savings.TariffConfig

class SettingsViewModel(val dataRepository: DataRepository) : ViewModel() {

    companion object {
        const val DEFAULT_PRODUCTION_NOISE_THRESHOLD = 5
    }

    private val _productionNoiseThreshold = MutableStateFlow(DEFAULT_PRODUCTION_NOISE_THRESHOLD)
    val productionNoiseThreshold: StateFlow<Int> = _productionNoiseThreshold

    private val _tariffConfig = MutableStateFlow(TariffConfig.defaults())
    val tariffConfig: StateFlow<TariffConfig> = _tariffConfig

    init {
        dataRepository.getSettings()
            .onEach { settings ->
                _productionNoiseThreshold.value =
                    settings.productionNoiseThreshold ?: DEFAULT_PRODUCTION_NOISE_THRESHOLD
                _tariffConfig.value = settings.tariffConfigJson?.let { TariffConfig.decode(it) }
                    ?: TariffConfig.defaults()
            }
            .launchIn(viewModelScope)
    }

    fun updateProductionNoiseThreshold(threshold: Int) {
        viewModelScope.launch {
            dataRepository.saveProductionNoiseThreshold(threshold)
        }
    }

    fun updateTariffConfig(config: TariffConfig) {
        _tariffConfig.value = config
        viewModelScope.launch {
            dataRepository.saveTariffConfig(config.copy(confirmedByUser = true))
        }
    }
}
