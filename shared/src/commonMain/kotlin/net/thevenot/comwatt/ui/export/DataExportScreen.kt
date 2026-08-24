package net.thevenot.comwatt.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.data_export_action
import comwatt.shared.generated.resources.data_export_cancel
import comwatt.shared.generated.resources.data_export_confirm
import comwatt.shared.generated.resources.data_export_estimate
import comwatt.shared.generated.resources.data_export_failed
import comwatt.shared.generated.resources.data_export_fetching
import comwatt.shared.generated.resources.data_export_invalid_range
import comwatt.shared.generated.resources.data_export_no_data
import comwatt.shared.generated.resources.data_export_pick_end
import comwatt.shared.generated.resources.data_export_pick_start
import comwatt.shared.generated.resources.data_export_range_1_year
import comwatt.shared.generated.resources.data_export_range_30_days
import comwatt.shared.generated.resources.data_export_range_3_months
import comwatt.shared.generated.resources.data_export_range_7_days
import comwatt.shared.generated.resources.data_export_range_custom
import comwatt.shared.generated.resources.data_export_range_label
import comwatt.shared.generated.resources.data_export_saved
import comwatt.shared.generated.resources.data_export_title
import comwatt.shared.generated.resources.data_export_writing
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.export.FileSaver
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun DataExportScreen(
    navController: NavController,
    dataRepository: DataRepository,
    fileSaver: FileSaver,
    viewModel: DataExportViewModel = viewModel { DataExportViewModel(dataRepository, fileSaver) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timeZone = TimeZone.currentSystemDefault()
    val range = remember(state.preset, state.customStart, state.customEnd) {
        resolveRange(state.preset, state.customStart, state.customEnd, viewModel.today(timeZone))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painter = AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(stringResource(Res.string.data_export_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.data_export_range_label),
                style = MaterialTheme.typography.labelLarge
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportRangePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = state.preset == preset,
                        onClick = { viewModel.onPresetSelected(preset) },
                        enabled = !state.isExporting,
                        label = { Text(preset.label()) }
                    )
                }
            }

            if (state.preset == ExportRangePreset.CUSTOM) {
                CustomRangePickers(
                    start = state.customStart,
                    end = state.customEnd,
                    enabled = !state.isExporting,
                    onRangeSelected = viewModel::onCustomRangeSelected
                )
            }

            if (range != null) {
                Text(
                    text = stringResource(
                        Res.string.data_export_estimate,
                        estimatedRowCount(range).toString()
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = stringResource(Res.string.data_export_invalid_range),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Cancel only while fetching: once Writing shows, the file is already written, so
            // there is nothing left to call off.
            if (state.status is ExportStatus.Fetching) {
                OutlinedButton(onClick = viewModel::cancel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.data_export_cancel))
                }
            } else if (!state.isExporting) {
                Button(
                    onClick = { viewModel.export(timeZone) },
                    enabled = range != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.data_export_action))
                }
            }

            ExportStatusRow(state.status)
        }
    }
}

@Composable
private fun ExportStatusRow(status: ExportStatus) {
    when (status) {
        ExportStatus.Idle -> Unit

        is ExportStatus.Fetching -> {
            Text(
                stringResource(Res.string.data_export_fetching, status.completed, status.total)
            )
            // Total is 0 until the device list lands; an indeterminate bar until then.
            if (status.total > 0) {
                LinearProgressIndicator(
                    progress = { status.completed.toFloat() / status.total },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        ExportStatus.Writing -> {
            Text(stringResource(Res.string.data_export_writing))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        is ExportStatus.Saved ->
            Text(stringResource(Res.string.data_export_saved, status.fileName))

        ExportStatus.NoData ->
            Text(stringResource(Res.string.data_export_no_data))

        is ExportStatus.Failed -> Text(
            text = stringResource(Res.string.data_export_failed, status.message),
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ExportRangePreset.label(): String = when (this) {
    ExportRangePreset.LAST_7_DAYS -> stringResource(Res.string.data_export_range_7_days)
    ExportRangePreset.LAST_30_DAYS -> stringResource(Res.string.data_export_range_30_days)
    ExportRangePreset.LAST_3_MONTHS -> stringResource(Res.string.data_export_range_3_months)
    ExportRangePreset.LAST_YEAR -> stringResource(Res.string.data_export_range_1_year)
    ExportRangePreset.CUSTOM -> stringResource(Res.string.data_export_range_custom)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangePickers(
    start: LocalDate?,
    end: LocalDate?,
    enabled: Boolean,
    onRangeSelected: (LocalDate, LocalDate) -> Unit
) {
    var editing by remember { mutableStateOf<CustomBound?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { editing = CustomBound.START },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(start?.toString() ?: stringResource(Res.string.data_export_pick_start))
        }
        OutlinedButton(
            onClick = { editing = CustomBound.END },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(end?.toString() ?: stringResource(Res.string.data_export_pick_end))
        }
    }

    val bound = editing ?: return
    // No point offering days that have not happened yet — they would export as NoData.
    val pickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= Clock.System.now().toEpochMilliseconds()
        }
    )
    DatePickerDialog(
        onDismissRequest = { editing = null },
        confirmButton = {
            TextButton(onClick = {
                val millis = pickerState.selectedDateMillis
                if (millis != null) {
                    val picked = Instant.fromEpochMilliseconds(millis)
                        .toLocalDateTime(TimeZone.UTC)
                        .date
                    when (bound) {
                        CustomBound.START -> onRangeSelected(picked, end ?: picked)
                        CustomBound.END -> onRangeSelected(start ?: picked, picked)
                    }
                }
                editing = null
            }) { Text(stringResource(Res.string.data_export_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { editing = null }) {
                Text(stringResource(Res.string.data_export_cancel))
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

private enum class CustomBound { START, END }
