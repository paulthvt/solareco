package net.thevenot.comwatt.ui.devices.settings.planning.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.navigation.NavController
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.error_fetching_data
import comwatt.shared.generated.resources.planning_mode_none
import comwatt.shared.generated.resources.typical_day_add_range
import comwatt.shared.generated.resources.typical_day_discard_cancel
import comwatt.shared.generated.resources.typical_day_discard_confirm
import comwatt.shared.generated.resources.typical_day_discard_message
import comwatt.shared.generated.resources.typical_day_discard_title
import comwatt.shared.generated.resources.typical_day_duplicate_suffix
import comwatt.shared.generated.resources.typical_day_editor_title
import comwatt.shared.generated.resources.typical_day_label
import comwatt.shared.generated.resources.typical_day_no_ranges
import comwatt.shared.generated.resources.typical_day_save
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import kotlinx.coroutines.flow.firstOrNull
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchDevicePlanningUseCase
import net.thevenot.comwatt.domain.SaveDeviceScheduleUseCase
import net.thevenot.comwatt.domain.SaveTypicalDayUseCase
import net.thevenot.comwatt.domain.TimelineBand
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.toTimelineBands
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.devices.settings.planning.TimelinePreviewBar
import net.thevenot.comwatt.ui.devices.settings.planning.color
import net.thevenot.comwatt.ui.devices.settings.planning.displayName
import net.thevenot.comwatt.ui.devices.settings.planning.hhmm
import net.thevenot.comwatt.ui.nav.Screen
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypicalDayEditorScreen(
    navController: NavController,
    route: Screen.TypicalDayEditor,
    dataRepository: DataRepository,
) {
    var siteIdResolved by remember(route) { mutableStateOf(false) }
    val siteId by produceState<Int?>(initialValue = null, route) {
        value = dataRepository.getSettings().firstOrNull()?.siteId
        siteIdResolved = true
    }

    if (!siteIdResolved) {
        EditorShell(onNavigateBack = { navController.popBackStack() }) {
            LoadingView(isLoading = true) { }
        }
        return
    }

    val currentSiteId = siteId
    if (currentSiteId == null) {
        EditorShell(onNavigateBack = { navController.popBackStack() }) {
            Text(
                text = stringResource(Res.string.error_fetching_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    EditorContent(
        route = route,
        siteId = currentSiteId,
        dataRepository = dataRepository,
        onNavigateBack = { navController.popBackStack() },
    )
}

/** Minimal Scaffold with the standard editor top bar, used for pre-content states. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorShell(
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = AppIcons.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = { Text(stringResource(Res.string.typical_day_editor_title)) },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun EditorContent(
    route: Screen.TypicalDayEditor,
    siteId: Int,
    dataRepository: DataRepository,
    onNavigateBack: () -> Unit,
    viewModel: TypicalDayEditorViewModel = viewModel(
        key = "editor_${route.deviceId}_${route.scheduleIndex}",
    ) {
        TypicalDayEditorViewModel(
            route = route,
            siteId = siteId,
            fetchDevicePlanningUseCase = FetchDevicePlanningUseCase(dataRepository.api),
            saveTypicalDayUseCase = SaveTypicalDayUseCase(dataRepository.api),
            saveDeviceScheduleUseCase = SaveDeviceScheduleUseCase(dataRepository.api),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(route) { viewModel.load() }

    var showDiscardPrompt by remember { mutableStateOf(false) }

    val onBackRequested = {
        if (uiState.isDirty) showDiscardPrompt = true else onNavigateBack()
    }

    BackHandler(enabled = uiState.isDirty) { showDiscardPrompt = true }

    if (showDiscardPrompt) {
        AlertDialog(
            onDismissRequest = { showDiscardPrompt = false },
            title = { Text(stringResource(Res.string.typical_day_discard_title)) },
            text = { Text(stringResource(Res.string.typical_day_discard_message)) },
            confirmButton = {
                TextButton(onClick = onNavigateBack) {
                    Text(stringResource(Res.string.typical_day_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardPrompt = false }) {
                    Text(stringResource(Res.string.typical_day_discard_cancel))
                }
            },
        )
    }

    if (uiState.needsSharingWarning) {
        val copiedLabel =
            stringResource(Res.string.typical_day_duplicate_suffix, uiState.label)
        SharedDayWarningSheet(
            deviceCount = uiState.sharingCount,
            onDismiss = { viewModel.acknowledgeSharing() },
            onEditAnyway = { viewModel.acknowledgeSharing() },
            onDuplicate = { viewModel.duplicateForThisDevice(copiedLabel) },
        )
    }

    uiState.editingIndex?.let { index ->
        uiState.ranges.getOrNull(index)?.let { range ->
            TimeRangeEditSheet(
                range = range,
                bounds = uiState.boundsFor(index),
                onDismiss = { viewModel.cancelEdit() },
                onConfirm = { viewModel.applyRange(index, it) },
                onDelete = { viewModel.deleteRange(index) },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackRequested) {
                        Icon(
                            painter = AppIcons.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = { Text(stringResource(Res.string.typical_day_editor_title)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = uiState.label,
                    onValueChange = { viewModel.setLabel(it) },
                    label = { Text(stringResource(Res.string.typical_day_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { TimelinePreviewBar(ranges = uiState.ranges, height = 26.dp) }

            item { HourAxis() }

            if (uiState.ranges.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.typical_day_no_ranges),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.ranges.isNotEmpty()) {
                itemsIndexed(uiState.ranges.toTimelineBands()) { _, band ->
                    val rangeIndex = band.sourceRangeIndex
                    if (band.mode == null || rangeIndex == null) {
                        GapRow(band)
                    } else {
                        RangeRow(
                            range = TimeRange(band.start, band.end, band.mode),
                            onClick = { viewModel.beginEdit(rangeIndex) },
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { viewModel.addRange() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.typical_day_add_range))
                }
            }

            uiState.error?.let { error ->
                item {
                    Text(
                        text = stringResource(error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            item {
                Button(
                    onClick = { viewModel.save(onDone = onNavigateBack) },
                    enabled = uiState.canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.typical_day_save))
                }
            }
        }
    }
}

/** One tappable range. Modes are named, never shown as their API values. */
@Composable
private fun RangeRow(range: TimeRange, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = range.mode.color(),
            modifier = Modifier.size(10.dp),
        ) {}
        Text(
            text = "${range.start.hhmm()} – ${range.end.hhmm()}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Box(modifier = Modifier.weight(1f))
        Text(
            text = range.mode.displayName(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A stretch of the day no range covers: Comwatt applies no rule, device holds its state. */
@Composable
private fun GapRow(band: TimelineBand) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(10.dp),
        ) {}
        Text(
            text = "${band.start.hhmm()} – ${band.end.hhmm()}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.planning_mode_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HourAxis() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("0h", "6h", "12h", "18h", "24h").forEach {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
