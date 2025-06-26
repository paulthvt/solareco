package net.thevenot.comwatt.ui.dashboard.pickers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.hour_range_dialog_picker_yesterday_label
import comwatt.composeapp.generated.resources.hour_range_selected_time
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.utils.formatTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HourPicker(
    currentDateTime: LocalDateTime,
    defaultSelectedTimeRange: Int,
    onIntervalSelected: (Int) -> Unit
) {
    val yesterdayLabel = stringResource(Res.string.hour_range_dialog_picker_yesterday_label)
    val items = computeItems(currentDateTime, yesterdayLabel)
    val selectedIntervalIndex = remember { mutableStateOf(defaultSelectedTimeRange) }
    val lazyListState = rememberLazyListState()
    val selectedItemIndex = items.indexOfFirst {
        it is ListItem.TimeInterval && it.index == defaultSelectedTimeRange
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
                    is ListItem.DateSeparator -> {
                        DaySeparator(item)
                    }

                    is ListItem.TimeInterval -> {
                        val intervalIndex = items.subList(0, index)
                            .count { it is ListItem.TimeInterval }
                        val isSelected = intervalIndex == selectedIntervalIndex.value

                        HourRangeButton(
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
private fun HourRangeButton(
    isSelected: Boolean,
    intervalIndex: Int,
    item: ListItem.TimeInterval,
    onSelected: (Int) -> Unit = {},
) {
    val buttonColors = if (isSelected) {
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        ButtonDefaults.outlinedButtonBorder()
    }

    OutlinedButton(
        onClick = { onSelected(intervalIndex) },
        colors = buttonColors,
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = AppTheme.dimens.paddingTooSmall,
                horizontal = AppTheme.dimens.paddingNormal
            ),
        shape = MaterialTheme.shapes.small
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
                text = "${formatTime(item.startTime)} - ${formatTime(item.endTime)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun DaySeparator(item: ListItem.DateSeparator) {
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
    val result = mutableListOf<ListItem>()
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
            result.add(ListItem.DateSeparator(yesterdayLabel))
        }

        result.add(ListItem.TimeInterval(startLocalTime, endLocalTime, intervalIndex))
        intervalIndex++
    }
    result
}

sealed class ListItem {
    data class TimeInterval(
        val startTime: LocalDateTime,
        val endTime: LocalDateTime,
        val index: Int
    ) : ListItem()

    data class DateSeparator(val label: String) : ListItem()
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