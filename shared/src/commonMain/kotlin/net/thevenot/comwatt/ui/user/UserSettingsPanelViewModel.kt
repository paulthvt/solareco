package net.thevenot.comwatt.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository

class UserSettingsPanelViewModel(
    private val dataRepository: DataRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserSettingsPanelState())
    val uiState: StateFlow<UserSettingsPanelState> get() = _uiState

    fun loadSite() {
        viewModelScope.launch {
            val siteId = dataRepository.getSettings().firstOrNull()?.siteId
            when (val siteResponse = dataRepository.api.sites()) {
                is Either.Left -> {
                    Logger.e(TAG) { "Error loading sites: ${siteResponse.value}" }
                }

                is Either.Right -> {
                    val site = siteResponse.value.firstOrNull { it -> it.id == siteId }
                    _uiState.update {
                        it.copy(
                            siteName = site?.name ?: "",
                            userName = "${site?.owner?.firstName} ${site?.owner?.lastName}"
                        )
                    }
                    Logger.d(TAG) { "Loaded site: $site" }
                }
            }
        }
    }

    fun logout(onLogoutCompleted: () -> Unit) {
        viewModelScope.launch {
            dataRepository.api.logout().onLeft {
                Logger.e(TAG) { "Error during logout: $it" }
            }.onRight {
                dataRepository.getUser()?.let {
                    dataRepository.removeUser(it)
                }
                onLogoutCompleted()
            }
        }
    }

    fun clearSite(onClearSiteCompleted: () -> Unit) {
        viewModelScope.launch {
            dataRepository.clearSiteId()
            onClearSiteCompleted()
        }
    }

    companion object {
        const val TAG = "UserSettingsPanelViewModel"
    }
}