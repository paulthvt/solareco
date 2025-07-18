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

@OptIn(FormatStringsInDatetimeFormats::class)
fun Instant.toZoneString(timeZone: TimeZone = TimeZone.UTC): String {
    val offset = timeZone.offsetAt(this)
    return this.format(DateTimeComponents.Format { byUnicodePattern("yyyy-MM-dd'T'HH:mm:ssxxx") }, offset)
}

fun LocalDateTime.formatHourMinutes(): String {
    val format = LocalDateTime.Format {
        hour(); char('h'); minute()
    }
    return this.format(format)
}

fun Instant.formatHourMinutes(): String {
    val format = DateTimeComponents.Format {
        hour(); char('h'); minute()
    }
    return this.format(format)
}

fun LocalTime.formatHourMinutes(): String {
    val customFormat = LocalTime.Format {
        hour(); char('h'); minute()
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