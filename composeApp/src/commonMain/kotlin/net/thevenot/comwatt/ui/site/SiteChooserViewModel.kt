package net.thevenot.comwatt.ui.site

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.client.Session
import net.thevenot.comwatt.model.Site
import net.thevenot.comwatt.model.User

class SiteChooserViewModel(
    private val session: Session,
    private val dataRepository: DataRepository
) : ViewModel() {
    private val _sites = MutableStateFlow<List<Site>>(listOf())
    val sites: StateFlow<List<Site>> = _sites

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
            _sites.value = dataRepository.api.sites(session.token)
            _user.value = dataRepository.api.authenticated(session.token)
        }
    }

    fun saveSiteId(siteId: Int) {
        viewModelScope.launch {
            dataRepository.saveSiteId(siteId)
        }
    }
}