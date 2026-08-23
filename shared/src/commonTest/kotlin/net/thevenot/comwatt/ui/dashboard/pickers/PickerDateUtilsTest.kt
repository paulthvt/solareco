package net.thevenot.comwatt.ui.dashboard.pickers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class PickerDateUtilsTest {
    @Test
    fun dayOffsetCountsWholeDaysAcrossMonths() {
        val selected = LocalDate(2026, 1, 17)
        val current = LocalDate(2026, 8, 23)

        assertEquals(218, dayOffsetBetween(selected, current))
    }

    @Test
    fun dayOffsetIsZeroForToday() {
        val current = LocalDate(2026, 8, 23)

        assertEquals(0, dayOffsetBetween(current, current))
    }

    @Test
    fun dayOffsetCountsWholeDaysAcrossYears() {
        assertEquals(365, dayOffsetBetween(LocalDate(2025, 8, 23), LocalDate(2026, 8, 23)))
    }

    @Test
    fun datePickerMillisRoundTripKeepsTheDate() {
        val date = LocalDate(2026, 1, 17)

        assertEquals(date, datePickerMillisToLocalDate(date.toDatePickerMillis()))
    }

    @Test
    fun datePickerMillisIsUtcMidnight() {
        val millis = LocalDate(2026, 1, 17).toDatePickerMillis()

        assertEquals(
            "2026-01-17T00:00",
            Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).toString()
        )
    }

    @Test
    fun todayAndPastDatesAreSelectableButFutureIsNot() {
        val today = LocalDate(2026, 8, 23)

        assertTrue(isSelectableDatePickerMillis(today.toDatePickerMillis(), today))
        assertTrue(isSelectableDatePickerMillis(LocalDate(2026, 1, 17).toDatePickerMillis(), today))
        assertFalse(isSelectableDatePickerMillis(LocalDate(2026, 8, 24).toDatePickerMillis(), today))
    }
}
