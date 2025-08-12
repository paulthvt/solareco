package net.thevenot.comwatt.utils

import kotlinx.datetime.DayOfWeek
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

/**
 * iOS implementation using NSDateFormatter
 */
actual object DateFormatter {
    actual fun getDayName(dayOfWeek: DayOfWeek): String {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.currentLocale

        // Convert kotlinx DayOfWeek to iOS weekday (1=Sunday, 2=Monday, etc.)
        val weekdayIndex = when (dayOfWeek) {
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            DayOfWeek.TUESDAY -> 3
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 5
            DayOfWeek.FRIDAY -> 6
            DayOfWeek.SATURDAY -> 7
        }

        return formatter.weekdaySymbols[weekdayIndex - 1] as String
    }

    actual fun getShortDayName(dayOfWeek: DayOfWeek): String {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale.currentLocale

        // Convert kotlinx DayOfWeek to iOS weekday (1=Sunday, 2=Monday, etc.)
        val weekdayIndex = when (dayOfWeek) {
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            DayOfWeek.TUESDAY -> 3
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 5
            DayOfWeek.FRIDAY -> 6
            DayOfWeek.SATURDAY -> 7
        }

        return formatter.shortWeekdaySymbols[weekdayIndex - 1] as String
    }
}
