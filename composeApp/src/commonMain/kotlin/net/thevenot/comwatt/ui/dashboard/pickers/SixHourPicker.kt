package net.thevenot.comwatt.ui.dashboard.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.hour_range_dialog_picker_yesterday_label
import comwatt.composeapp.generated.resources.six_hour_range_selected_time
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.utils.formatHourMinutes
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock

@Composable
fun SixHourPicker(
    currentDateTime: LocalDateTime,
    defaultSelectedTimeRange: Int,
    onIntervalSelected: (Int) -> Unit
) {
    val yesterdayLabel = stringResource(Res.string.hour_range_dialog_picker_yesterday_label)
    val items = computeItems(currentDateTime, yesterdayLabel)
    val selectedIntervalIndex = remember { mutableStateOf(defaultSelectedTimeRange) }
    val lazyListState = rememberLazyListState()
    val selectedItemIndex = items.indexOfFirst {
        it is SixHourListItem.TimeInterval && it.index == defaultSelectedTimeRange
    }.coerceAtLeast(0)

    LaunchedEffect(Unit) {
        lazyListState.scrollToItem(selectedItemIndex)
    }

    Column {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            items(items.size) { index ->
                when (val item = items[index]) {
                    is SixHourListItem.DateSeparator -> {
                        DaySeparator(item)
                    }

                    is SixHourListItem.TimeInterval -> {
                        val intervalIndex = items.subList(0, index)
                            .count { it is SixHourListItem.TimeInterval }
                        val isSelected = intervalIndex == selectedIntervalIndex.value

                        SixHourRangeButton(
                            isSelected = isSelected,
                            intervalIndex = intervalIndex,
                            item = item
                        ) { selectedIndex ->
                            selectedIntervalIndex.value = selectedIndex
                            onIntervalSelected(selectedIndex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SixHourRangeButton(
    isSelected: Boolean,
    intervalIndex: Int,
    item: SixHourListItem.TimeInterval,
    onSelected: (Int) -> Unit = {},
) {
    ToggleButton(
        checked = isSelected, onCheckedChange = {
            onSelected(intervalIndex)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = AppTheme.dimens.paddingTooSmall,
                horizontal = AppTheme.dimens.paddingNormal
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = pluralStringResource(
                    Res.plurals.six_hour_range_selected_time,
                    (intervalIndex * 3) + 6,
                    (intervalIndex * 3) + 6
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${item.startTime.formatHourMinutes()} - ${item.endTime.formatHourMinutes()}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun DaySeparator(item: SixHourListItem.DateSeparator) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimens.paddingSmall)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = AppTheme.dimens.paddingNormal)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun computeItems(currentDateTime: LocalDateTime, yesterdayLabel: String) = remember {
    val result = mutableListOf<SixHourListItem>()
    val currentDay = currentDateTime.date
    var previousDay = currentDay
    var intervalIndex = 0

    // Create intervals: 6h, 9h, 12h, 15h, 18h, 21h, 24h ago, etc.
    // Each interval is 6 hours long, starting at 3-hour increments
    (0..7).forEach { stepOffset ->
        val currentTime = currentDateTime.toInstant(TimeZone.currentSystemDefault())
        val endTime = currentTime.minus(stepOffset * 3, DateTimeUnit.HOUR)
        val startTime = currentTime.minus((stepOffset * 3) + 6, DateTimeUnit.HOUR)

        val startLocalTime = startTime.toLocalDateTime(TimeZone.currentSystemDefault())
        val endLocalTime = endTime.toLocalDateTime(TimeZone.currentSystemDefault())

        if (startLocalTime.date != currentDay && startLocalTime.date != previousDay) {
            previousDay = startLocalTime.date
            result.add(SixHourListItem.DateSeparator(yesterdayLabel))
        }

        result.add(SixHourListItem.TimeInterval(startLocalTime, endLocalTime, intervalIndex))
        intervalIndex++
    }
    result
}

sealed class SixHourListItem {
    data class TimeInterval(
        val startTime: LocalDateTime,
        val endTime: LocalDateTime,
        val index: Int
    ) : SixHourListItem()

    data class DateSeparator(val label: String) : SixHourListItem()
}

@Preview
@Composable
fun SixHourPickerPreview() {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    ComwattTheme {
        SixHourPicker(
            currentDateTime = now,
            defaultSelectedTimeRange = 0,
            onIntervalSelected = { _ -> }
        )
    }
}
