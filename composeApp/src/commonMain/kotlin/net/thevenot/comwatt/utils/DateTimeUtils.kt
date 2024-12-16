package net.thevenot.comwatt.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.offsetAt

@OptIn(FormatStringsInDatetimeFormats::class)
fun Instant.toZoneString(timeZone: TimeZone = TimeZone.UTC): String {
    val offset = timeZone.offsetAt(this)
    return this.format(DateTimeComponents.Format { byUnicodePattern("yyyy-MM-dd'T'HH:mm:ssxxx") }, offset)
}