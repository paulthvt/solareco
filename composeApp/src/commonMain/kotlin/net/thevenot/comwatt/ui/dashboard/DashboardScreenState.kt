package net.thevenot.comwatt.ui.dashboard

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.domain.model.SiteDailyData
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

data class DashboardScreenState(
    val isRefreshing: Boolean = false,
    val lastErrorMessage: String = "",
    val selectedTimeUnit: DashboardTimeUnit = DashboardTimeUnit.HOUR,
    val isDataLoaded: Boolean = false,
    val callCount: Int = 0,
    val selectedTimeRange: SelectedTimeRange = SelectedTimeRange(),
    val rangeStats: SiteDailyData? = null,
    val expandedCards: Set<String> = emptySet()
)

data class SelectedTimeRange(
    val hour: HourRange = HourRange.fromSelectedValue(0),
    val day: DayRange = DayRange.fromSelectedValue(0),
    val week: WeekRange = WeekRange.fromSelectedValue(0),
    val custom: CustomRange = CustomRange.fromSelectedValues(
        Clock.System.now().minus(7, DateTimeUnit.DAY, TimeZone.currentSystemDefault()),
        Clock.System.now()
    )
) {
    fun withUpdatedRange(
        hourSelectedValue: Int = hour.selectedValue,
        daySelectedValue: Int = day.selectedValue,
        weekSelectedValue: Int = week.selectedValue,
        customSelectedStartValue: Instant = custom.selectedStartValue,
        customSelectedEndValue: Instant = custom.selectedEndValue
    ): SelectedTimeRange {
        return this
            .withUpdatedHourRange(hourSelectedValue)
            .withUpdatedDayRange(daySelectedValue)
            .withUpdatedWeekRange(weekSelectedValue)
            .withUpdatedCustomRange(customSelectedStartValue, customSelectedEndValue)
    }

    fun withUpdatedHourRange(selectedValue: Int = hour.selectedValue): SelectedTimeRange {
        return this.copy(
            hour = HourRange.fromSelectedValue(selectedValue)
        )
    }

    fun withUpdatedDayRange(selectedValue: Int = day.selectedValue): SelectedTimeRange {
        return this.copy(
            day = DayRange.fromSelectedValue(selectedValue)
        )
    }

    fun withUpdatedWeekRange(selectedValue: Int = week.selectedValue): SelectedTimeRange {
        return this.copy(
            week = WeekRange.fromSelectedValue(selectedValue)
        )
    }

    fun withUpdatedCustomRange(
        selectedStartValue: Instant = custom.selectedStartValue,
        selectedEndValue: Instant = custom.selectedEndValue
    ): SelectedTimeRange {
        return this.copy(
            custom = CustomRange.fromSelectedValues(selectedStartValue, selectedEndValue)
        )
    }
}

data class HourRange(
    val selectedValue: Int,
    val start: LocalDateTime,
    val end: LocalDateTime
) {
    companion object {
        fun fromSelectedValue(selectedValue: Int): HourRange {
            val currentTimeZone = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
            val targetHourStart = now.minus(selectedValue + 1, DateTimeUnit.HOUR)

            return HourRange(
                selectedValue,
                targetHourStart.toLocalDateTime(currentTimeZone),
                now.toLocalDateTime(currentTimeZone)
            )
        }
    }
}

data class DayRange(
    val selectedValue: Int,
    val start: LocalDateTime,
    val end: LocalDateTime,
) {
    companion object {
        fun fromSelectedValue(selectedValue: Int): DayRange {
            val currentTimeZone = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
            val end = now.minus(selectedValue, DateTimeUnit.DAY, currentTimeZone)
            val start = end.minus(1, DateTimeUnit.DAY, currentTimeZone)

            return DayRange(
                selectedValue,
                start.toLocalDateTime(currentTimeZone),
                end.toLocalDateTime(currentTimeZone)
            )
        }
    }
}

data class WeekRange(
    val selectedValue: Int,
    val start: LocalDateTime,
    val end: LocalDateTime
) {
    companion object {
        fun fromSelectedValue(selectedValue: Int): WeekRange {
            val currentTimeZone = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
            val localNow = now.toLocalDateTime(currentTimeZone)

            if (selectedValue == 0) {
                // Last 7 days (today - 6 days to today)
                val end = now
                val start = now.minus(6, DateTimeUnit.DAY, currentTimeZone)
                return WeekRange(
                    selectedValue,
                    start.toLocalDateTime(currentTimeZone),
                    end.toLocalDateTime(currentTimeZone)
                )
            } else {
                // Sunday-to-Sunday pattern for previous weeks
                val localDate = localNow.date

                // Find previous Sunday
                var mostRecentSunday = localDate
                while (mostRecentSunday.dayOfWeek != DayOfWeek.SUNDAY) {
                    mostRecentSunday = mostRecentSunday.minus(1, DateTimeUnit.DAY)
                }

                // Calculate end date (Sunday)
                val endLocalDate = mostRecentSunday.minus((selectedValue - 1) * 7, DateTimeUnit.DAY)
                // Calculate start date (Monday, 6 days before)
                val startLocalDate = endLocalDate.minus(6, DateTimeUnit.DAY)

                // Convert LocalDate to Instant (end of day for end date)
                val start = startLocalDate.atStartOfDayIn(currentTimeZone)
                val end = endLocalDate.atStartOfDayIn(currentTimeZone)
                    .plus(1, DateTimeUnit.DAY, currentTimeZone)
                    .minus(1, DateTimeUnit.NANOSECOND)

                return WeekRange(
                    selectedValue,
                    start.toLocalDateTime(currentTimeZone),
                    end.toLocalDateTime(currentTimeZone)
                )
            }
        }
    }
}

data class CustomRange(
    val selectedStartValue: Instant,
    val selectedEndValue: Instant,
    val start: LocalDateTime,
    val end: LocalDateTime
) {
    companion object {
        fun fromSelectedValues(
            selectedStartValue: Instant,
            selectedEndValue: Instant
        ): CustomRange {
            return CustomRange(
                selectedStartValue = selectedStartValue,
                selectedEndValue = selectedEndValue,
                start = selectedStartValue.toLocalDateTime(TimeZone.currentSystemDefault()),
                end = selectedEndValue.toLocalDateTime(TimeZone.currentSystemDefault())
            )
        }
    }
}