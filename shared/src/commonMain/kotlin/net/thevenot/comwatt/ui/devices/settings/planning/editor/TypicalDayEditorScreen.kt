package net.thevenot.comwatt.ui.devices.settings.planning.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.error_fetching_data
import comwatt.shared.generated.resources.planning_mode_none
import comwatt.shared.generated.resources.typical_day_add_range
import comwatt.shared.generated.resources.typical_day_add_rule
import comwatt.shared.generated.resources.typical_day_discard_cancel
import comwatt.shared.generated.resources.typical_day_discard_confirm
import comwatt.shared.generated.resources.typical_day_discard_message
import comwatt.shared.generated.resources.typical_day_discard_title
import comwatt.shared.generated.resources.typical_day_duplicate_suffix
import comwatt.shared.generated.resources.typical_day_editor_title
import comwatt.shared.generated.resources.typical_day_label
import comwatt.shared.generated.resources.typical_day_delete_range
import comwatt.shared.generated.resources.typical_day_no_ranges
import comwatt.shared.generated.resources.typical_day_rule_hint
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
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.devices.settings.planning.TimelinePreviewBar
import net.thevenot.comwatt.ui.devices.settings.planning.color
import net.thevenot.comwatt.ui.devices.settings.planning.displayName
import net.thevenot.comwatt.ui.devices.settings.planning.durationLabel
import net.thevenot.comwatt.ui.devices.settings.planning.icon
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

            item {
                Text(
                    text = stringResource(
                        if (uiState.ranges.isEmpty()) {
                            Res.string.typical_day_no_ranges
                        } else {
                            Res.string.typical_day_rule_hint
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Every slot of the day is a row, gaps included: the previous list
            // showed gaps too but left them inert, so a user tapping one of the
            // three visible rows got a response from only the middle one.
            // Keyed by the slot's start so a deleted or split slot animates in
            // place: without a key every row below the change would be recycled
            // and the list would jump instead of sliding.
            // Rule slots key on their source range index and gaps on their start
            // — both unique per list, where start alone would collide if two
            // ranges ever shared one and crash the list.
            items(
                uiState.bands,
                key = { it.sourceRangeIndex?.let { index -> "rule-$index" } ?: "gap-${it.start}" },
            ) { band ->
                val rangeIndex = band.sourceRangeIndex
                if (band.mode == null || rangeIndex == null) {
                    GapCard(
                        band = band,
                        onClick = { viewModel.addRangeCovering(band.start, band.end) },
                        modifier = Modifier.animateItem(),
                    )
                } else {
                    SwipeToDeleteSlot(
                        onDelete = { viewModel.deleteRange(rangeIndex) },
                        modifier = Modifier.animateItem(),
                    ) {
                        RuleCard(
                            range = TimeRange(band.start, band.end, band.mode),
                            onClick = { viewModel.beginEdit(rangeIndex) },
                            onDelete = { viewModel.deleteRange(rangeIndex) },
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { viewModel.addRange() },
                    enabled = uiState.canAddRange,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = AppIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                    // A disabled button alone reads as "nothing happened". The
                    // spinner slides in beside the unchanged label so the row
                    // never jumps and the wording stays stable.
                    AnimatedVisibility(visible = uiState.isSaving) {
                        Row {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = LocalContentColor.current,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    Text(stringResource(Res.string.typical_day_save))
                }
            }
        }
    }
}

private val CARD_SHAPE = RoundedCornerShape(16.dp)

/**
 * Swipe either way to delete the wrapped slot. The card keeps its own delete
 * button as well: a swipe is undiscoverable on its own, and pointer platforms
 * (desktop) make it awkward.
 *
 * Deletion is committed straight away because it is not yet persisted — Save
 * writes the day, and leaving the editor still prompts to discard — so an undo
 * affordance here would only shadow the one the screen already has.
 */
@Composable
private fun SwipeToDeleteSlot(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.Settled) {
                false
            } else {
                onDelete()
                true
            }
        },
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            // The bin sits on whichever edge the card is being pulled away from,
            // so the gesture names itself before it completes.
            val alignment = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CARD_SHAPE)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                Icon(
                    painter = AppIcons.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        content = { content() },
    )
}

/**
 * One covered slot. A card rather than a bare row so the tap highlight is clipped
 * to a rounded shape — a full-bleed rectangular ripple over a flat row read as a
 * rendering glitch. Delete sits on the card as its own button: reaching it
 * through the edit sheet made removing a slot feel like a hidden gesture.
 */
@Composable
private fun RuleCard(
    range: TimeRange,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The rail and glyph cross-fade between modes, so changing a slot's mode in
    // the sheet reads as the same slot changing rather than a new row.
    val accent by animateColorAsState(
        targetValue = range.mode.color(),
        animationSpec = tween(durationMillis = 280),
    )

    Card(
        onClick = onClick,
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Colour rail instead of a dot: it ties the row to its band in the
            // strip above and survives being read at a glance.
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(accent))

            Row(
                modifier = Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Crossfade(targetState = range.mode) { mode ->
                    Icon(
                        painter = mode.icon(),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${range.start.hhmm()} – ${range.end.hhmm()}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    AnimatedContent(
                        targetState = "${range.mode.displayName()} · ${
                            durationLabel(range.start, range.end)
                        }",
                    ) { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.padding(end = 4.dp)) {
                Icon(
                    painter = AppIcons.Delete,
                    contentDescription = stringResource(Res.string.typical_day_delete_range),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * A stretch of the day no rule covers: the device holds whatever state it was
 * already in. Tapping it claims the whole stretch, so the row that explains the
 * gap is also the way to close it — outlined and dimmer than a rule card so the
 * two never read as the same kind of thing.
 */
@Composable
private fun GapCard(band: TimelineBand, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${band.start.hhmm()} – ${band.end.hhmm()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${stringResource(Res.string.planning_mode_none)} · ${
                        durationLabel(band.start, band.end)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = AppIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(Res.string.typical_day_add_rule),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
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
