package net.thevenot.comwatt.ui.dashboard

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.day_range_dialog_picker_confirm_button
import comwatt.composeapp.generated.resources.day_range_dialog_picker_dismiss_button
import comwatt.composeapp.generated.resources.day_range_dialog_picker_title
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.ui.dashboard.pickers.CustomPicker
import net.thevenot.comwatt.ui.dashboard.pickers.DayPicker
import net.thevenot.comwatt.ui.dashboard.pickers.HourPicker
import net.thevenot.comwatt.ui.dashboard.pickers.WeekPicker
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
fun TimePickerDialog(
    selectedTimeUnit: DashboardTimeUnit,
    onDismiss: () -> Unit,
    defaultSelectedTimeRange: SelectedTimeRange,
    onRangeSelected: (SelectedTimeRange) -> Unit
) {
    val currentDateTime =
        remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }

    var selectedHour by remember { mutableStateOf(defaultSelectedTimeRange.hour.selectedValue) }
    var selectedDay by remember { mutableStateOf(defaultSelectedTimeRange.day.selectedValue) }
    var selectedWeek by remember { mutableStateOf(defaultSelectedTimeRange.week.selectedValue) }
    var selectedCustomStart by
    remember { mutableStateOf(defaultSelectedTimeRange.custom.selectedStartValue) }
    var selectedCustomEnd by
    remember { mutableStateOf(defaultSelectedTimeRange.custom.selectedStartValue) }
    var isRangeValid by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    when (selectedTimeUnit) {
                        DashboardTimeUnit.HOUR -> {
                            onRangeSelected(
                                SelectedTimeRange(
                                    hour = HourRange.fromSelectedValue(
                                        selectedHour
                                    )
                                )
                            )
                        }

                        DashboardTimeUnit.DAY -> {
                            onRangeSelected(
                                SelectedTimeRange(
                                    day = DayRange.fromSelectedValue(
                                        selectedDay
                                    )
                                )
                            )
                        }

                        DashboardTimeUnit.WEEK -> {
                            onRangeSelected(
                                SelectedTimeRange(
                                    week = WeekRange.fromSelectedValue(
                                        selectedWeek
                                    )
                                )
                            )
                        }

                        DashboardTimeUnit.CUSTOM -> {
                            onRangeSelected(
                                SelectedTimeRange(
                                    custom = CustomRange.fromSelectedValues(
                                        selectedCustomStart,
                                        selectedCustomEnd
                                    )
                                )
                            )
                        }
                    }

                    onDismiss()
                },
                enabled = isRangeValid
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
            if (selectedTimeUnit != DashboardTimeUnit.DAY) {
                Text(stringResource(Res.string.day_range_dialog_picker_title))
            }
        },
        text = {
            when (selectedTimeUnit) {
                DashboardTimeUnit.HOUR -> HourPicker(
                    currentDateTime = currentDateTime,
                    defaultSelectedTimeRange = defaultSelectedTimeRange.hour.selectedValue,
                    onIntervalSelected = { range ->
                        selectedHour = range
                    }
                )

                DashboardTimeUnit.DAY -> DayPicker(
                    currentDateTime = currentDateTime,
                    defaultSelectedDay = defaultSelectedTimeRange.day.selectedValue,
                    onDateSelected = { day ->
                        selectedDay = day
                    }
                )

                DashboardTimeUnit.WEEK -> WeekPicker(
                    currentDateTime = currentDateTime,
                    defaultSelectedWeek = defaultSelectedTimeRange.week.selectedValue,
                    onIntervalSelected = { week ->
                        selectedWeek = week
                    }
                )

                DashboardTimeUnit.CUSTOM -> CustomPicker(
                    currentDateTime = currentDateTime,
                    defaultStartDateTime = defaultSelectedTimeRange.custom.selectedStartValue,
                    defaultEndDateTime = defaultSelectedTimeRange.custom.selectedEndValue,
                    onRangeSelected = { range ->
                        isRangeValid = range.isRangeValid
                        if (range.isRangeValid) {
                            selectedCustomStart = range.start
                            selectedCustomEnd = range.end
                        }
                    }
                )
            }
        }
    )
}