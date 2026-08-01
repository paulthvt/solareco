package net.thevenot.comwatt.ui.devices.settings.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.error_fetching_data
import comwatt.shared.generated.resources.planning_add_schedule
import comwatt.shared.generated.resources.planning_no_schedules
import kotlinx.coroutines.flow.firstOrNull
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchDevicePlanningUseCase
import net.thevenot.comwatt.domain.SaveDeviceScheduleUseCase
import net.thevenot.comwatt.ui.common.LoadingView
import org.jetbrains.compose.resources.stringResource

/**
 * @param onEditTypicalDay called with the tapped schedule's index in
 *   [PlanningState.userSchedules] and its typical day id, if it has one
 */
@Composable
fun PlanningTab(
    deviceId: Int,
    dataRepository: DataRepository,
    onEditTypicalDay: (Int, Int?) -> Unit,
) {
    var siteIdResolved by remember(deviceId) { mutableStateOf(false) }
    val siteId by produceState<Int?>(initialValue = null, deviceId) {
        value = dataRepository.getSettings().firstOrNull()?.siteId
        siteIdResolved = true
    }

    if (!siteIdResolved) {
        LoadingView(isLoading = true) { }
        return
    }

    val currentSiteId = siteId
    if (currentSiteId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.error_fetching_data),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    PlanningTabContent(deviceId, currentSiteId, dataRepository, onEditTypicalDay)
}

@Composable
private fun PlanningTabContent(
    deviceId: Int,
    siteId: Int,
    dataRepository: DataRepository,
    onEditTypicalDay: (Int, Int?) -> Unit,
    viewModel: PlanningViewModel = viewModel(key = "planning_$deviceId") {
        PlanningViewModel(
            deviceId = deviceId,
            siteId = siteId,
            fetchDevicePlanningUseCase = FetchDevicePlanningUseCase(dataRepository.api),
            saveDeviceScheduleUseCase = SaveDeviceScheduleUseCase(dataRepository.api),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(deviceId) { viewModel.load() }

    val hasLoadError = uiState.hasError && uiState.planning == null

    // LoadingView only renders its error branch while isLoading is also true, and
    // other screens rely on that, so the flag is forced here instead. Without it
    // a failed load would fall through to an empty list with an Add button, which
    // reads as "this device has no schedules".
    LoadingView(
        isLoading = uiState.isLoading || hasLoadError,
        hasError = hasLoadError,
        onRefresh = { viewModel.load() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(uiState.userSchedules) { index, schedule ->
                ScheduleCard(
                    schedule = schedule,
                    sharingCount = uiState.sharingCount(schedule.typicalDay.id),
                    onEdit = { onEditTypicalDay(index, schedule.typicalDay.id) },
                    onDelete = { viewModel.deleteSchedule(schedule) },
                )
            }

            items(uiState.serverSchedules) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    sharingCount = 0,
                    onEdit = {},
                    onDelete = {},
                )
            }

            if (uiState.userSchedules.isEmpty() && uiState.serverSchedules.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.planning_no_schedules),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                OutlinedButton(onClick = { onEditTypicalDay(-1, null) }) {
                    Text(stringResource(Res.string.planning_add_schedule))
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
