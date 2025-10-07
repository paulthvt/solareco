package net.thevenot.comwatt.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DateTimeUtilsTest {
    @Test
    fun testToZoneString() {
        val testCases = listOf(
            Pair("2024-12-29T10:17:21Z", "Europe/Paris") to "2024-12-29T11:17:21+01:00",
            Pair("2024-06-29T10:17:21Z", "Europe/Paris") to "2024-06-29T12:17:21+02:00",
            Pair("2024-12-29T10:17:21Z", "UTC") to "2024-12-29T10:17:21+00:00",
            Pair("2024-12-29T10:17:21Z", "America/New_York") to "2024-12-29T05:17:21-05:00"
        )

        for ((input, expected) in testCases) {
            val (instantString, timeZoneId) = input
            val instant = Instant.parse(instantString)
            val timeZone = TimeZone.of(timeZoneId)
            val result = instant.toZoneString(timeZone)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun testParseTimestamp() {
        val instant = Instant.parse("2025-01-20T19:49:23.009Z")

    }

    @Test
    fun testLocalDateTimeFormatDayMonth() {
        val testCases = listOf(
            LocalDateTime(2024, 1, 5, 10, 30) to "5 Jan",
            LocalDateTime(2024, 12, 25, 0, 0) to "25 Dec",
            LocalDateTime(2025, 6, 15, 14, 45) to "15 Jun",
            LocalDateTime(2025, 10, 7, 22, 30) to "7 Oct",
            LocalDateTime(2024, 2, 1, 8, 15) to "1 Feb"
        )

        for ((localDateTime, expected) in testCases) {
            val result = localDateTime.formatDayMonth()
            assertEquals(expected, result, "Failed for input: $localDateTime")
        }
    }
}