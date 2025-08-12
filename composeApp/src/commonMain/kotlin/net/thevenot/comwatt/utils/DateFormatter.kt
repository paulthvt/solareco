package net.thevenot.comwatt.utils

import kotlinx.datetime.DayOfWeek

/**
 * Platform-specific date formatter for getting localized day names
 */
expect object DateFormatter {
    /**
     * Get localized day name for the given day of week
     * @param dayOfWeek the day of the week
     * @return localized day name (e.g., "Monday", "Lundi", etc.)
     */
    fun getDayName(dayOfWeek: DayOfWeek): String

    /**
     * Get short localized day name for the given day of week
     * @param dayOfWeek the day of the week
     * @return short localized day name (e.g., "Mon", "Lun", etc.)
     */
    fun getShortDayName(dayOfWeek: DayOfWeek): String
}
