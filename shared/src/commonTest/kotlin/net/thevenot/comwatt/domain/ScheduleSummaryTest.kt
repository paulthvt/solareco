package net.thevenot.comwatt.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScheduleSummaryTest {

    /** A Wednesday. */
    private val today = LocalDate(2026, 7, 29)

    private fun schedule(
        ranges: List<TimeRange>,
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        start: LocalDate = LocalDate(2026, 1, 1),
        end: LocalDate = LocalDate(2026, 12, 31),
        isServerManaged: Boolean = false,
    ) = DeviceSchedule(
        id = null,
        typicalDay = TypicalDay(id = 1, label = "d", ranges = ranges, isServerManaged = isServerManaged),
        days = days,
        startDate = start,
        endDate = end,
        isServerManaged = isServerManaged,
    )

    private val solarMidday = TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR)

    @Test
    fun `picks the range covering now`() {
        val summary = listOf(schedule(listOf(solarMidday))).summaryFor(today, LocalTime(12, 0))
        assertEquals(ScheduleSummary(ScheduleMode.SOLAR, LocalTime(10, 0), LocalTime(17, 0)), summary)
    }

    @Test
    fun `returns null when no range covers now`() {
        assertNull(listOf(schedule(listOf(solarMidday))).summaryFor(today, LocalTime(8, 0)))
    }

    @Test
    fun `ignores a schedule not active on today's weekday`() {
        val weekendOnly = schedule(listOf(solarMidday), days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        assertNull(listOf(weekendOnly).summaryFor(today, LocalTime(12, 0)))
    }

    @Test
    fun `ignores a schedule outside its date window`() {
        val expired = schedule(
            listOf(solarMidday),
            start = LocalDate(2025, 1, 1),
            end = LocalDate(2025, 12, 31),
        )
        assertNull(listOf(expired).summaryFor(today, LocalTime(12, 0)))
    }

    @Test
    fun `includes the window boundary days`() {
        val endsToday = schedule(listOf(solarMidday), start = today, end = today)
        assertEquals(
            ScheduleSummary(ScheduleMode.SOLAR, LocalTime(10, 0), LocalTime(17, 0)),
            listOf(endsToday).summaryFor(today, LocalTime(12, 0)),
        )
    }

    @Test
    fun `a server managed schedule wins over a user one`() {
        val user = schedule(listOf(TimeRange(LocalTime(0, 0), LocalTime(0, 0), ScheduleMode.OFF)))
        val server = schedule(listOf(solarMidday), isServerManaged = true)
        val summary = listOf(user, server).summaryFor(today, LocalTime(12, 0))
        assertEquals(ScheduleMode.SOLAR, summary?.mode)
    }

    @Test
    fun `a range ending at midnight covers the evening`() {
        val evening = schedule(listOf(TimeRange(LocalTime(18, 0), LocalTime(0, 0), ScheduleMode.ON)))
        assertEquals(
            ScheduleSummary(ScheduleMode.ON, LocalTime(18, 0), LocalTime(0, 0)),
            listOf(evening).summaryFor(today, LocalTime(23, 30)),
        )
    }

    @Test
    fun `an empty list has no summary`() {
        assertNull(emptyList<DeviceSchedule>().summaryFor(today, LocalTime(12, 0)))
    }
}
