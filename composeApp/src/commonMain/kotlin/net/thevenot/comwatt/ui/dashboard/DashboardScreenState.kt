package net.thevenot.comwatt.ui.dashboard

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus

data class DashboardScreenState(
    val isRefreshing: Boolean = false,
    val timeUnitSelectedIndex: Int = 0,
    val isDataLoaded: Boolean = false,
    val callCount: Int = 0,
    val errorCount: Int = 0,
    val selectedTimeRange: SelectedTimeRange = SelectedTimeRange(),
)

data class SelectedTimeRange(
    val hour: HourRange = HourRange.fromSelectedValue(0),
    val day: DayRange = DayRange.fromSelectedValue(0),
    val week: Int = 0
) {
    fun withUpdatedRange(
        hourSelectedValue: Int = hour.selectedValue,
        daySelectedValue: Int = day.selectedValue
    ): SelectedTimeRange {
        return this
            .withUpdatedHourRange(hourSelectedValue)
            .withUpdatedDayRange(daySelectedValue)
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
}

data class HourRange(
    val selectedValue: Int,
    val start: Instant,
    val end: Instant
) {
    companion object {
        fun fromSelectedValue(selectedValue: Int): HourRange {
            val now = Clock.System.now()
            val start = now.minus(selectedValue + 1, DateTimeUnit.HOUR)
            val end = now.minus(selectedValue, DateTimeUnit.HOUR)
            return HourRange(selectedValue, start, end)
        }
    }
}

data class DayRange(
    val selectedValue: Int,
    val value: Instant,
) {
    companion object {
        fun fromSelectedValue(selectedValue: Int): DayRange {
            val now = Clock.System.now()
            val value = now.minus(selectedValue, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            return DayRange(selectedValue, value)
        }
    }
}