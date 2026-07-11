package net.thevenot.comwatt.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.database.SolarEcoSettings
import net.thevenot.comwatt.domain.FetchCurrentSiteUseCase
import net.thevenot.comwatt.domain.savings.ComputeSavingsUseCase
import net.thevenot.comwatt.model.savings.TariffConfig

class SavingsViewModel(
    private val computeSavingsUseCase: ComputeSavingsUseCase,
    private val siteIdProvider: suspend () -> Int?,
    private val settingsFlow: () -> Flow<SolarEcoSettings>,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavingsScreenState())
    val uiState: StateFlow<SavingsScreenState> get() = _uiState

    constructor(dataRepository: DataRepository) : this(
        computeSavingsUseCase = ComputeSavingsUseCase(dataRepository),
        siteIdProvider = {
            val fetchCurrentSiteUseCase = FetchCurrentSiteUseCase(dataRepository)
            when (val result = fetchCurrentSiteUseCase.invoke()) {
                is Either.Right -> result.value?.id
                is Either.Left -> null
            }
        },
        settingsFlow = { dataRepository.getSettings() }
    )

    init {
        refresh()
    }

    // TODO(Task 7/8): Restore period selection once SavingsPeriod is reimplemented
    // fun selectPeriod(period: SavingsPeriod) {
    //     _uiState.update { it.copy(period = period) }
    //     refresh()
    // }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            val settings = settingsFlow().first()
            val config = settings.tariffConfigJson?.let { TariffConfig.decode(it) }
                ?: TariffConfig.defaults()
            val siteId = siteIdProvider()
            if (siteId == null) {
                _uiState.update { it.copy(isLoading = false, hasError = true) }
                return@launch
            }

            // TODO(Task 7/8): Replace placeholder period with real UI period selection
            val now = Clock.System.now()
            val zone = TimeZone.currentSystemDefault()
            val start = now  // Placeholder: use today's start
            val end = now    // Placeholder: use now

            val result = computeSavingsUseCase(
                siteId = siteId,
                start = start,
                end = end,
                config = config,
                zone = zone,
            )
            _uiState.update {
                when (result) {
                    is Either.Right -> it.copy(
                        isLoading = false,
                        hasError = false,
                        breakdown = result.value,
                        config = config,
                        configConfirmed = config.confirmedByUser
                    )
                    is Either.Left -> it.copy(
                        isLoading = false,
                        hasError = true,
                        config = config,
                        configConfirmed = config.confirmedByUser
                    )
                }
            }
        }
    }
}
