package net.thevenot.comwatt.ui.site

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.model.SiteDto
import net.thevenot.comwatt.model.UserDto

class SiteChooserViewModel(
    private val dataRepository: DataRepository
) : ViewModel() {
    private val _sites = MutableStateFlow<List<SiteDto>>(listOf())
    val sites: StateFlow<List<SiteDto>> = _sites

    private val _userDto = MutableStateFlow<UserDto?>(null)
    val userDto: StateFlow<UserDto?> = _userDto

    fun checkSiteSelection(onSiteSelected: (Int) -> Unit, onNoSiteSelected: () -> Unit) {
        viewModelScope.launch {
            val siteId = dataRepository.getSettings().firstOrNull()?.siteId
            if (siteId == null) {
                onNoSiteSelected()
            } else {
                onSiteSelected(siteId)
            }
        }
    }

    fun loadSites() {
        viewModelScope.launch {
            when (val siteResponse = dataRepository.api.sites()) {
                is Either.Left -> {
                    Logger.e(TAG) { "Error loading sites: ${siteResponse.value}" }
                }
                is Either.Right -> {
                    _sites.value = siteResponse.value
                }
            }

            when (val userResponse = dataRepository.api.authenticated()) {
                is Either.Left -> {
                    Logger.e(TAG) { "Error loading user: ${userResponse.value}" }
                }
                is Either.Right -> {
                    _userDto.value = userResponse.value
                }
            }
        }
    }

    fun saveSiteId(siteId: Int) {
        viewModelScope.launch {
            dataRepository.saveSiteId(siteId)
        }
    }

    companion object {
        const val TAG = "SiteChooserViewModel"
    }
}