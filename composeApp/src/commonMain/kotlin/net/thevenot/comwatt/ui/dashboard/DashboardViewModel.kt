package net.thevenot.comwatt.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.model.DeviceDto

class DashboardViewModel(dataRepository: DataRepository): ViewModel() {
    private val _devices = MutableStateFlow<List<DeviceDto>>(listOf())
    val devices: StateFlow<List<DeviceDto>> = _devices

    init {
        viewModelScope.launch {
            val siteId = dataRepository.getSettings().firstOrNull()?.siteId
            siteId?.let { id ->
                val fetchDevices = dataRepository.api.fetchDevices(id)
                Napier.d(tag = "DashboardViewModel") { "devices: $fetchDevices" }
                fetchDevices.onRight {
                    _devices.value = it
                }
            }
        }
    }
}