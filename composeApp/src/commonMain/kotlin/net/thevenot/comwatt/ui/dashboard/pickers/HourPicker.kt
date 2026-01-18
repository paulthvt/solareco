package net.thevenot.comwatt.ui.dashboard.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.hour_range_dialog_picker_yesterday_label
import comwatt.composeapp.generated.resources.hour_range_selected_time
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.utils.formatHourMinutes
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
fun HourPicker(
    currentDateTime: LocalDateTime,
    defaultSelectedTimeRange: Int,
    onIntervalSelected: (Int) -> Unit
) {
    val yesterdayLabel = stringResource(Res.string.hour_range_dialog_picker_yesterday_label)
    val items = computeItems(currentDateTime, yesterdayLabel)

    IntervalPicker(
        items = items,
        defaultSelectedIndex = defaultSelectedTimeRange,
        onIntervalSelected = onIntervalSelected,
        buttonContent = { intervalIndex, _, data ->
            val timeInterval = data as HourInterval
            HourRangeButtonContent(intervalIndex, timeInterval)
        }
    )
}

@Composable
private fun HourRangeButtonContent(
    intervalIndex: Int,
    timeInterval: HourInterval
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = pluralStringResource(
                Res.plurals.hour_range_selected_time,
                intervalIndex + 1,
                intervalIndex + 1
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "${timeInterval.startTime.formatHourMinutes()} - ${timeInterval.endTime.formatHourMinutes()}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

data class HourInterval(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
)

@Composable
private fun computeItems(currentDateTime: LocalDateTime, yesterdayLabel: String) = remember {
    val result = mutableListOf<PickerListItem>()
    val currentDay = currentDateTime.date
    var previousDay = currentDay
    var intervalIndex = 0

    (0..23).forEach { hourOffset ->
        val currentTime = currentDateTime.toInstant(TimeZone.currentSystemDefault())
        val endTime = currentTime.minus(hourOffset, DateTimeUnit.HOUR)
        val startTime = currentTime.minus(hourOffset + 1, DateTimeUnit.HOUR)

        val startLocalTime = startTime.toLocalDateTime(TimeZone.currentSystemDefault())
        val endLocalTime = endTime.toLocalDateTime(TimeZone.currentSystemDefault())

        if (startLocalTime.date != currentDay && startLocalTime.date != previousDay) {
            previousDay = startLocalTime.date
            result.add(PickerListItem.SeparatorItem(yesterdayLabel))
        }

        result.add(
            PickerListItem.IntervalItem(
                index = intervalIndex,
                data = HourInterval(startLocalTime, endLocalTime)
            )
        )
        intervalIndex++
    }
    result
}

@Preview
@Composable
fun HourPickerPreview() {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    ComwattTheme {
        HourPicker(
            currentDateTime = now,
            defaultSelectedTimeRange = 0,
            onIntervalSelected = { _ -> }
        )
    }
}