package net.thevenot.comwatt.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.thevenot.comwatt.DataRepository

/**
 * ViewModel for managing widget updates
 */
class WidgetViewModel(
    private val dataRepository: DataRepository,
    private val widgetManager: WidgetManager
) : ViewModel() {

    private val logger = Logger.withTag("WidgetViewModel")

    private val _updateState = MutableStateFlow<WidgetUpdateState>(WidgetUpdateState.Idle)
    val updateState: StateFlow<WidgetUpdateState> = _updateState.asStateFlow()

    /**
     * Manually trigger widget update
     */
    fun updateWidget() {
        viewModelScope.launch {
            _updateState.value = WidgetUpdateState.Loading

            try {
                // Get current site ID from settings
                val settings = dataRepository.getSettings().firstOrNull()
                val siteId = settings?.siteId

                if (siteId == null) {
                    _updateState.value = WidgetUpdateState.Error("No site selected")
                    logger.w { "Cannot update widget: no site selected" }
                    return@launch
                }

                // Update widget data
                when (val result = widgetManager.updateWidgetData(siteId)) {
                    is Either.Left -> {
                        _updateState.value = WidgetUpdateState.Error(result.value)
                        logger.e { "Widget update failed: ${result.value}" }
                    }

                    is Either.Right -> {
                        _updateState.value = WidgetUpdateState.Success
                        logger.d { "Widget updated successfully" }
                    }
                }
            } catch (e: Exception) {
                _updateState.value = WidgetUpdateState.Error(e.message ?: "Unknown error")
                logger.e(e) { "Exception during widget update" }
            }
        }
    }

    /**
     * Enable automatic widget updates
     */
    fun enableAutomaticUpdates() {
        try {
            widgetManager.schedulePeriodicUpdates()
            logger.d { "Automatic widget updates enabled" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to enable automatic updates" }
        }
    }

    /**
     * Disable automatic widget updates
     */
    fun disableAutomaticUpdates() {
        try {
            widgetManager.cancelPeriodicUpdates()
            logger.d { "Automatic widget updates disabled" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to disable automatic updates" }
        }
    }

    fun resetState() {
        _updateState.value = WidgetUpdateState.Idle
    }
}

/**
 * Widget update state
 */
sealed class WidgetUpdateState {
    data object Idle : WidgetUpdateState()
    data object Loading : WidgetUpdateState()
    data object Success : WidgetUpdateState()
    data class Error(val message: String) : WidgetUpdateState()
}
