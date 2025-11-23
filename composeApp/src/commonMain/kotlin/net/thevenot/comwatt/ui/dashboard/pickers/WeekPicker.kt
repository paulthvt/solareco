package net.thevenot.comwatt.ui.dashboard.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.week_range_selected_time_n_weeks_ago
import comwatt.composeapp.generated.resources.week_range_selected_time_one_week_ago
import comwatt.composeapp.generated.resources.week_range_selected_time_past_seven_days
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock

@Composable
fun WeekPicker(
    currentDateTime: LocalDateTime,
    defaultSelectedWeek: Int,
    onIntervalSelected: (Int) -> Unit
) {
    val defaultWeeksCount = 10
    val initialWeeksCount =
        if (defaultSelectedWeek > defaultWeeksCount) defaultSelectedWeek + defaultWeeksCount else defaultWeeksCount
    var items by remember { mutableStateOf(computeWeekItems(currentDateTime, initialWeeksCount)) }
    val selectedWeekIndex = remember { mutableStateOf(defaultSelectedWeek) }
    val lazyListState = rememberLazyListState()

    val selectedItemIndex = remember(items, defaultSelectedWeek) {
        items.indexOfFirst {
            it is WeekListItem.WeekInterval && it.index == defaultSelectedWeek
        }.coerceAtLeast(0)
    }

    LaunchedEffect(Unit) {
        lazyListState.scrollToItem(selectedItemIndex)
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= items.size - 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && items.isNotEmpty()) {
            val lastWeek = items.last()
            if (lastWeek is WeekListItem.WeekInterval) {
                val nextWeekIndex = lastWeek.index + 1
                val moreItems = loadMoreWeeks(currentDateTime, nextWeekIndex, 5)
                items = items + moreItems
            }
        }
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
                    is WeekListItem.MonthSeparator -> {
                        MonthSeparator(item)
                    }

                    is WeekListItem.WeekInterval -> {
                        val weekIndex = item.index
                        val isSelected = weekIndex == selectedWeekIndex.value

                        WeekRangeButton(
                            isSelected = isSelected,
                            item = item
                        ) {
                            selectedWeekIndex.value = weekIndex
                            onIntervalSelected(weekIndex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekRangeButton(
    isSelected: Boolean,
    item: WeekListItem.WeekInterval,
    onSelected: () -> Unit = {},
) {
    val weekTitle = when (item.index) {
        0 -> stringResource(Res.string.week_range_selected_time_past_seven_days)
        1 -> stringResource(Res.string.week_range_selected_time_one_week_ago)
        else -> stringResource(
            Res.string.week_range_selected_time_n_weeks_ago,
            item.index
        )
    }

    ToggleButton(
        checked = isSelected, onCheckedChange = {
            onSelected()
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
                text = weekTitle,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${formatDate(item.startDate)} - ${formatDate(item.endDate)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun MonthSeparator(item: WeekListItem.MonthSeparator) {
    DateSeparator(item.label)
}

private fun formatDate(date: LocalDate): String {
    return date.format(
        LocalDate.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            dayOfMonth()
        })
}

private fun getPreviousSunday(date: LocalDate): LocalDate {
    var currentDate = date
    while (currentDate.dayOfWeek != DayOfWeek.SUNDAY) {
        currentDate = currentDate.minus(1, DateTimeUnit.DAY)
    }
    return currentDate
}

private fun computeWeekItems(currentDateTime: LocalDateTime, weeksCount: Int): List<WeekListItem> {
    val result = mutableListOf<WeekListItem>()
    val currentDate = currentDateTime.date

    // First week (index 0): Last 7 days
    val firstWeekEndDate = currentDate
    val firstWeekStartDate = firstWeekEndDate.minus(6, DateTimeUnit.DAY)
    var previousMonth = firstWeekStartDate.monthNumber

    if (firstWeekStartDate.monthNumber != firstWeekEndDate.monthNumber) {
        result.add(WeekListItem.MonthSeparator("${firstWeekStartDate.month} ${firstWeekStartDate.year}"))
    }

    result.add(WeekListItem.WeekInterval(firstWeekStartDate, firstWeekEndDate, 0))

    // Find the previous Sunday for subsequent weeks
    val mostRecentSunday = getPreviousSunday(currentDate)

    // For weeks with index 1 and above: Sunday to Sunday pattern
    for (weekOffset in 1 until weeksCount) {
        val endDate = mostRecentSunday.minus((weekOffset - 1) * 7, DateTimeUnit.DAY)
        val startDate = endDate.minus(6, DateTimeUnit.DAY)

        if (startDate.monthNumber != previousMonth) {
            previousMonth = startDate.monthNumber
            result.add(WeekListItem.MonthSeparator("${startDate.month} ${startDate.year}"))
        }

        result.add(WeekListItem.WeekInterval(startDate, endDate, weekOffset))
    }

    return result
}

private fun loadMoreWeeks(
    currentDateTime: LocalDateTime,
    existingWeeksCount: Int,
    additionalWeeks: Int
): List<WeekListItem> {
    val result = mutableListOf<WeekListItem>()
    val currentDate = currentDateTime.date
    val mostRecentSunday = getPreviousSunday(currentDate)

    var previousMonth = -1 // Will be set on first iteration

    for (i in 0 until additionalWeeks) {
        val weekOffset = existingWeeksCount + i
        val endDate = mostRecentSunday.minus((weekOffset - 1) * 7, DateTimeUnit.DAY)
        val startDate = endDate.minus(6, DateTimeUnit.DAY)

        if (previousMonth == -1 || startDate.monthNumber != previousMonth) {
            previousMonth = startDate.monthNumber
            result.add(WeekListItem.MonthSeparator("${startDate.month} ${startDate.year}"))
        }

        result.add(WeekListItem.WeekInterval(startDate, endDate, weekOffset))
    }

    return result
}

sealed class WeekListItem {
    data class WeekInterval(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val index: Int
    ) : WeekListItem()

    data class MonthSeparator(val label: String) : WeekListItem()
}

@Preview
@Composable
fun WeekPickerPreview() {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    ComwattTheme {
        WeekPicker(
            currentDateTime = now,
            defaultSelectedWeek = 0,
            onIntervalSelected = { _ -> }
        )
    }
}