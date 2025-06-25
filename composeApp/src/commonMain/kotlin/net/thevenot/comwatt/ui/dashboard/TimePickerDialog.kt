package net.thevenot.comwatt.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.day_range_dialog_picker_confirm_button
import comwatt.composeapp.generated.resources.day_range_dialog_picker_dismiss_button
import comwatt.composeapp.generated.resources.day_range_dialog_picker_title
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.ui.dashboard.pickers.DayPicker
import net.thevenot.comwatt.ui.dashboard.pickers.HourPicker
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimePickerDialog(
    selectedTimeUnitIndex: Int,
    onDismiss: () -> Unit,
    defaultSelectedTimeRange: SelectedTimeRange,
    onRangeSelected: (SelectedTimeRange) -> Unit
) {
    val currentDateTime =
        remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }

    val selectedHourRange = remember { mutableStateOf(defaultSelectedTimeRange.hour.selectedValue) }
    val selectedDay = remember { mutableStateOf(defaultSelectedTimeRange.day.selectedValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    when (selectedTimeUnitIndex) {
                        0 -> {
                            onRangeSelected(
                                SelectedTimeRange(
                                    hour = HourRange.fromSelectedValue(
                                        selectedHourRange.value
                                    )
                                )
                            )
                        }

                        1 -> {
                            onRangeSelected(
                                SelectedTimeRange(
                                    day = DayRange.fromSelectedValue(
                                        selectedDay.value
                                    )
                                )
                            )
                        }
                    }

                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.day_range_dialog_picker_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.day_range_dialog_picker_dismiss_button))
            }
        },
        title = {
            if (selectedTimeUnitIndex != 1) {
                Text(stringResource(Res.string.day_range_dialog_picker_title))
            }
        },
        text = {
            when (selectedTimeUnitIndex) {
                0 -> HourPicker(
                    currentDateTime = currentDateTime,
                    defaultSelectedTimeRange = defaultSelectedTimeRange.hour.selectedValue,
                    onIntervalSelected = { range ->
                        selectedHourRange.value = range
                    }
                )

                1 -> DayPicker(
                    currentDateTime = currentDateTime,
                    defaultSelectedDay = defaultSelectedTimeRange.day.selectedValue,
                    onDateSelected = { day ->
                        selectedDay.value = day
                    }
                )

                2 -> WeekPicker(currentDateTime, onRangeSelected, onDismiss)
                else -> Text("Custom date selection not implemented")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekPicker(
    currentDateTime: LocalDateTime,
    onDateSelected: (SelectedTimeRange) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    Column {
        Text("Select a 7-day period ending no later than today")
        Spacer(modifier = Modifier.height(8.dp))

        DateRangePicker(
            state = dateRangePickerState,
//            dateValidator = { timestamp ->
//                timestamp <= currentDateTime.toInstant(TimeZone.currentSystemDefault())
//                    .toEpochMilliseconds()
//            },
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = {
                dateRangePickerState.selectedEndDateMillis?.let { endMillis ->
                    // Use the end date as the selected date
                    onDateSelected(SelectedTimeRange(week = 0))
                }
            },
            enabled = isValidWeekSelection(dateRangePickerState, currentDateTime),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Confirm")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun isValidWeekSelection(
    dateRangePickerState: DateRangePickerState,
    currentDateTime: LocalDateTime
): Boolean {
    val startMillis = dateRangePickerState.selectedStartDateMillis
    val endMillis = dateRangePickerState.selectedEndDateMillis

    if (startMillis == null || endMillis == null) return false

    val startDate = Instant.fromEpochMilliseconds(startMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    val endDate = Instant.fromEpochMilliseconds(endMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date

    val today = currentDateTime.date
    if (endDate > today) return false

    return endDate.minus(startDate).days == 6
}