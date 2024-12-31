package net.thevenot.comwatt.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

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
}