package net.thevenot.comwatt.ui.site

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.client.Session
import net.thevenot.comwatt.model.SiteDto
import net.thevenot.comwatt.model.User

class SiteChooserViewModel(
    private val session: Session,
    private val dataRepository: DataRepository
) : ViewModel() {
    private val _sites = MutableStateFlow<List<SiteDto>>(listOf())
    val sites: StateFlow<List<SiteDto>> = _sites

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

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
            when (val siteResponse = dataRepository.api.sites(session.token)) {
                is Either.Left -> {
                    Napier.e(tag = "SiteChooserViewModel") { "Error loading sites: ${siteResponse.value}" }
                }
                is Either.Right -> {
                    _sites.value = siteResponse.value
                }
            }

            _user.value = dataRepository.api.authenticated(session.token)
        }
    }

    fun saveSiteId(siteId: Int) {
        viewModelScope.launch {
            dataRepository.saveSiteId(siteId)
        }
    }
}