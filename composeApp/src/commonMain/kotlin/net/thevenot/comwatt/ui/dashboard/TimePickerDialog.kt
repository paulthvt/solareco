package net.thevenot.comwatt.ui.dashboard

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.day_range_dialog_picker_confirm_button
import comwatt.composeapp.generated.resources.day_range_dialog_picker_dismiss_button
import comwatt.composeapp.generated.resources.day_range_dialog_picker_title
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.ui.dashboard.pickers.DayPicker
import net.thevenot.comwatt.ui.dashboard.pickers.HourPicker
import net.thevenot.comwatt.ui.dashboard.pickers.WeekPicker
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
    val selectedWeek = remember { mutableStateOf(defaultSelectedTimeRange.week.selectedValue) }

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
                        2 -> {
                            onRangeSelected(
                                SelectedTimeRange(
                                    week = WeekRange.fromSelectedValue(
                                        selectedWeek.value
                                    )
                                )
                            )
                        }

                        else -> {
                            // Custom date selection not implemented
                            onRangeSelected(defaultSelectedTimeRange)
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

                2 -> WeekPicker(
                    currentDateTime = currentDateTime,
                    defaultSelectedWeek = defaultSelectedTimeRange.week.selectedValue,
                    onIntervalSelected = { week ->
                        selectedWeek.value = week
                    }
                )
                else -> Text("Custom date selection not implemented")
            }
        }
    )
}