package net.thevenot.comwatt.ui.devices.settings.planning.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.planning_mode_off
import comwatt.shared.generated.resources.planning_mode_on
import comwatt.shared.generated.resources.planning_mode_solar
import comwatt.shared.generated.resources.typical_day_delete_range
import comwatt.shared.generated.resources.typical_day_range_end
import comwatt.shared.generated.resources.typical_day_range_start
import comwatt.shared.generated.resources.typical_day_save
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import org.jetbrains.compose.resources.stringResource

/**
 * Edits one range. Both time steppers clamp to [bounds] so overlaps cannot be
 * created — the spec prefers this over validating on save, because a clamped
 * stepper cannot produce an invalid draft to explain.
 */
@Composable
fun TimeRangeEditSheet(
    range: TimeRange,
    bounds: Pair<LocalTime, LocalTime>,
    onDismiss: () -> Unit,
    onConfirm: (TimeRange) -> Unit,
    onDelete: () -> Unit,
) {
    var start by remember { mutableStateOf(range.start) }
    var end by remember { mutableStateOf(range.end) }
    var mode by remember { mutableStateOf(range.mode) }

    val (lower, upper) = bounds

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TimeStepperRow(
                label = stringResource(Res.string.typical_day_range_start),
                value = start,
                lower = lower,
                upper = end,
                onChange = { start = it },
            )

            TimeStepperRow(
                label = stringResource(Res.string.typical_day_range_end),
                value = end,
                lower = start,
                upper = upper,
                onChange = { end = it },
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(ScheduleMode.OFF, ScheduleMode.ON, ScheduleMode.SOLAR)
                modes.forEachIndexed { index, candidate ->
                    SegmentedButton(
                        selected = mode == candidate,
                        onClick = { mode = candidate },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        label = {
                            Text(
                                when (candidate) {
                                    ScheduleMode.OFF -> stringResource(Res.string.planning_mode_off)
                                    ScheduleMode.ON -> stringResource(Res.string.planning_mode_on)
                                    ScheduleMode.SOLAR -> stringResource(Res.string.planning_mode_solar)
                                }
                            )
                        },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(Res.string.typical_day_delete_range))
                }
                Button(
                    onClick = { onConfirm(TimeRange(start, end, mode)) },
                    enabled = start != end,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.typical_day_save))
                }
            }
        }
    }
}

/** 15-minute stepper. Clamped to [lower]..[upper], where 00:00 as [upper] means end of day. */
@Composable
private fun TimeStepperRow(
    label: String,
    value: LocalTime,
    lower: LocalTime,
    upper: LocalTime,
    onChange: (LocalTime) -> Unit,
) {
    val minMinutes = lower.toMinutes()
    val maxMinutes = if (upper == LocalTime(0, 0)) MINUTES_PER_DAY else upper.toMinutes()
    val current = value.toMinutes()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))

        TextButton(
            onClick = { onChange((current - STEP_MINUTES).coerceAtLeast(minMinutes).toLocalTime()) },
            enabled = current - STEP_MINUTES >= minMinutes,
        ) { Text("−") }

        Text(
            text = value.hhmm(),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )

        TextButton(
            onClick = { onChange((current + STEP_MINUTES).coerceAtMost(maxMinutes).toLocalTime()) },
            enabled = current + STEP_MINUTES <= maxMinutes,
        ) { Text("+") }
    }
}

private const val STEP_MINUTES = 15
private const val MINUTES_PER_DAY = 24 * 60

private fun LocalTime.toMinutes(): Int = hour * 60 + minute

/** 1440 wraps back to 00:00, which the model reads as end of day. */
private fun Int.toLocalTime(): LocalTime {
    val clamped = coerceIn(0, MINUTES_PER_DAY) % MINUTES_PER_DAY
    return LocalTime(clamped / 60, clamped % 60)
}

/** Formats a [LocalTime] as HH:MM. Shared with [TypicalDayEditorScreen]. */
internal fun LocalTime.hhmm(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
