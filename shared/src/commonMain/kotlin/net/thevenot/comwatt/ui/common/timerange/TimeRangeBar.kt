package net.thevenot.comwatt.ui.common.timerange

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.day_range_selected_time_n_days_gao
import comwatt.shared.generated.resources.day_range_selected_time_today
import comwatt.shared.generated.resources.day_range_selected_time_yesterday
import comwatt.shared.generated.resources.hour_range_selected_time
import comwatt.shared.generated.resources.range_picker_button_custom
import comwatt.shared.generated.resources.range_picker_button_day
import comwatt.shared.generated.resources.range_picker_button_hour
import comwatt.shared.generated.resources.range_picker_button_sixhour
import comwatt.shared.generated.resources.range_picker_button_week
import comwatt.shared.generated.resources.six_hour_range_selected_time
import comwatt.shared.generated.resources.week_range_selected_time_n_weeks_ago
import comwatt.shared.generated.resources.week_range_selected_time_one_week_ago
import comwatt.shared.generated.resources.week_range_selected_time_past_seven_days
import net.thevenot.comwatt.ui.dashboard.SelectedTimeRange
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import net.thevenot.comwatt.utils.formatDayMonth
import net.thevenot.comwatt.utils.formatHourMinutes
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimeUnitBar(
    selectedTimeUnit: DashboardTimeUnit,
    onTimeUnitSelected: (DashboardTimeUnit) -> Unit = {}
) {
    Row(
        Modifier
            .padding(horizontal = AppTheme.dimens.paddingSmall)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(
            space = ButtonGroupDefaults.ConnectedSpaceBetween,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        val options = listOf(
            stringResource(Res.string.range_picker_button_hour) to DashboardTimeUnit.HOUR,
            stringResource(Res.string.range_picker_button_sixhour) to DashboardTimeUnit.SIXHOUR,
            stringResource(Res.string.range_picker_button_day) to DashboardTimeUnit.DAY,
            stringResource(Res.string.range_picker_button_week) to DashboardTimeUnit.WEEK,
            stringResource(Res.string.range_picker_button_custom) to DashboardTimeUnit.CUSTOM
        )
        options.forEachIndexed { index, (label, timeUnit) ->
            ToggleButton(
                checked = timeUnit == selectedTimeUnit,
                onCheckedChange = { onTimeUnitSelected(timeUnit) },
                modifier = Modifier.semantics { role = Role.RadioButton },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                Text(label, maxLines = 1)
            }
        }
    }
}

@Composable
fun RangeButton(
    selectedTimeUnit: DashboardTimeUnit,
    selectedTimeRange: SelectedTimeRange,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onOpenPicker: () -> Unit
) {
    val selectedValue = when (selectedTimeUnit) {
        DashboardTimeUnit.HOUR -> selectedTimeRange.hour.selectedValue
        DashboardTimeUnit.SIXHOUR -> selectedTimeRange.sixHour.selectedValue
        DashboardTimeUnit.DAY -> selectedTimeRange.day.selectedValue
        DashboardTimeUnit.WEEK -> selectedTimeRange.week.selectedValue
        DashboardTimeUnit.CUSTOM -> 0
    }
    val minBound = when (selectedTimeUnit) {
        DashboardTimeUnit.HOUR -> 23
        DashboardTimeUnit.SIXHOUR -> 7
        DashboardTimeUnit.DAY -> 364
        DashboardTimeUnit.WEEK -> 52
        DashboardTimeUnit.CUSTOM -> 0
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.paddingNormal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedTimeUnit != DashboardTimeUnit.CUSTOM) {
            OutlinedIconButton(
                shape = ButtonDefaults.squareShape,
                onClick = onPrevious, enabled = selectedValue < minBound
            ) {
                Icon(AppIcons.ChevronLeft, contentDescription = "Previous")
            }
        }

        TextButton(onClick = onOpenPicker, modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.padding(AppTheme.dimens.paddingNormal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    when (selectedTimeUnit) {
                        DashboardTimeUnit.HOUR -> pluralStringResource(
                            Res.plurals.hour_range_selected_time,
                            selectedTimeRange.hour.selectedValue + 1,
                            selectedTimeRange.hour.selectedValue + 1
                        )

                        DashboardTimeUnit.SIXHOUR -> pluralStringResource(
                            Res.plurals.six_hour_range_selected_time,
                            (selectedTimeRange.sixHour.selectedValue * 3) + 6,
                            (selectedTimeRange.sixHour.selectedValue * 3) + 6
                        )

                        DashboardTimeUnit.DAY -> when (selectedTimeRange.day.selectedValue) {
                            0 -> stringResource(Res.string.day_range_selected_time_today)
                            1 -> stringResource(Res.string.day_range_selected_time_yesterday)
                            else -> stringResource(
                                Res.string.day_range_selected_time_n_days_gao,
                                selectedTimeRange.day.selectedValue
                            )
                        }

                        DashboardTimeUnit.WEEK -> when (selectedTimeRange.week.selectedValue) {
                            0 -> stringResource(Res.string.week_range_selected_time_past_seven_days)
                            1 -> stringResource(Res.string.week_range_selected_time_one_week_ago)
                            else -> stringResource(
                                Res.string.week_range_selected_time_n_weeks_ago,
                                selectedTimeRange.week.selectedValue
                            )
                        }

                        DashboardTimeUnit.CUSTOM -> "${selectedTimeRange.custom.start.formatDayMonth()} - ${
                            selectedTimeRange.custom.end.formatDayMonth()
                        }"
                    }
                )

                when (selectedTimeUnit) {
                    DashboardTimeUnit.HOUR -> Text(
                        text = "${selectedTimeRange.hour.start.formatHourMinutes()} - ${selectedTimeRange.hour.end.formatHourMinutes()}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    DashboardTimeUnit.SIXHOUR -> Text(
                        text = "${selectedTimeRange.sixHour.start.formatHourMinutes()} - ${selectedTimeRange.sixHour.end.formatHourMinutes()}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    DashboardTimeUnit.DAY -> Text(
                        text = selectedTimeRange.day.end.formatDayMonth(),
                        style = MaterialTheme.typography.bodySmall
                    )

                    DashboardTimeUnit.WEEK -> Text(
                        text = "${selectedTimeRange.week.start.formatDayMonth()} - ${
                            selectedTimeRange.week.end.formatDayMonth()
                        }", style = MaterialTheme.typography.bodySmall
                    )

                    DashboardTimeUnit.CUSTOM -> Text(
                        text = "${selectedTimeRange.custom.start.formatHourMinutes()} - ${selectedTimeRange.custom.end.formatHourMinutes()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (selectedTimeUnit != DashboardTimeUnit.CUSTOM) {
            OutlinedIconButton(
                shape = ButtonDefaults.squareShape,
                onClick = onNext,
                enabled = selectedValue > 0
            ) {
                Icon(AppIcons.ChevronRight, contentDescription = "Next")
            }
        }
    }
}
