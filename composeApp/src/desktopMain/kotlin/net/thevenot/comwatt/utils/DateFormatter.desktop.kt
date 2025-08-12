package net.thevenot.comwatt.utils

import kotlinx.datetime.DayOfWeek
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Desktop implementation using Calendar API (compatible approach)
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
        return when (dayOfWeek) {
            DayOfWeek.SUNDAY -> Calendar.SUNDAY
            DayOfWeek.MONDAY -> Calendar.MONDAY
            DayOfWeek.TUESDAY -> Calendar.TUESDAY
            DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
            DayOfWeek.THURSDAY -> Calendar.THURSDAY
            DayOfWeek.FRIDAY -> Calendar.FRIDAY
            DayOfWeek.SATURDAY -> Calendar.SATURDAY
        }
    }
}
