package net.thevenot.comwatt.utils

import kotlinx.datetime.DayOfWeek
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Android implementation using Calendar API (compatible with API 24+)
 */
actual object DateFormatter {
    actual fun getDayName(dayOfWeek: DayOfWeek): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, kotlinxDayOfWeekToCalendarDayOfWeek(dayOfWeek))
        val format = SimpleDateFormat("EEEE", Locale.getDefault())
        return format.format(calendar.time)
    }

    actual fun getShortDayName(dayOfWeek: DayOfWeek): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, kotlinxDayOfWeekToCalendarDayOfWeek(dayOfWeek))
        val format = SimpleDateFormat("EEE", Locale.getDefault())
        return format.format(calendar.time)
    }

    private fun kotlinxDayOfWeekToCalendarDayOfWeek(dayOfWeek: DayOfWeek): Int {
        return when (dayOfWeek.ordinal) {
            0 -> Calendar.MONDAY
            1 -> Calendar.TUESDAY
            2 -> Calendar.WEDNESDAY
            3 -> Calendar.THURSDAY
            4 -> Calendar.FRIDAY
            5 -> Calendar.SATURDAY
            6 -> Calendar.SUNDAY
            else -> throw IllegalArgumentException("Unknown day of week: $dayOfWeek")
        }
    }
}
