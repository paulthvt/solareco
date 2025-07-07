package net.thevenot.comwatt.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime

@OptIn(FormatStringsInDatetimeFormats::class)
fun Instant.toZoneString(timeZone: TimeZone = TimeZone.UTC): String {
    val offset = timeZone.offsetAt(this)
    return this.format(DateTimeComponents.Format { byUnicodePattern("yyyy-MM-dd'T'HH:mm:ssxxx") }, offset)
}

fun formatTime(dateTime: LocalDateTime): String {
    return "${dateTime.hour.toString().padStart(2, '0')}h${
        dateTime.minute.toString().padStart(2, '0')
    }"
}

fun formatTime(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    return formatTime(instant.toLocalDateTime(timeZone))
}

//@OptIn(FormatStringsInDatetimeFormats::class)
//fun Instant.formatTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
//    val offset = timeZone.offsetAt(this)
//    return this.format(DateTimeComponents.Format { byUnicodePattern("HH:mm") }, offset)
//}

fun LocalTime.formatHourMinutes(): String {
    val customFormat = LocalTime.Format {
        hour(); char(':'); minute()
    }
    return this.format(customFormat)
}

fun LocalDate.formatYearMonthDay(): String {
    return this.format(LocalDate.Formats.ISO)
}

@OptIn(FormatStringsInDatetimeFormats::class)
fun Instant.formatDayMonth(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val offset = timeZone.offsetAt(this)
    return this.format(DateTimeComponents.Format {
        dayOfMonth()
        char(' ')
        monthName(MonthNames.ENGLISH_ABBREVIATED)
    }, offset)
}