package net.thevenot.comwatt.model.savings

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class SavingsPeriodTest {
    private val utc = TimeZone.UTC
    // 2026-07-11T13:30:00Z
    private val now = Instant.parse("2026-07-11T13:30:00Z")

    @Test
    fun todayIsMidnightToNow() {
        val (start, end) = SavingsPeriod.Today.toRange(now, utc)
        assertEquals(Instant.parse("2026-07-11T00:00:00Z"), start)
        assertEquals(now, end)
    }

    @Test
    fun thisMonthStartsFirstOfMonth() {
        val (start, end) = SavingsPeriod.ThisMonth.toRange(now, utc)
        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), start)
        assertEquals(now, end)
    }

    @Test
    fun thisYearStartsFirstOfYear() {
        val (start, end) = SavingsPeriod.ThisYear.toRange(now, utc)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), start)
        assertEquals(now, end)
    }

    @Test
    fun customReturnsItsBounds() {
        val s = Instant.parse("2026-03-01T00:00:00Z")
        val e = Instant.parse("2026-04-01T00:00:00Z")
        val (start, end) = SavingsPeriod.Custom(s, e).toRange(now, utc)
        assertEquals(s, start)
        assertEquals(e, end)
    }
}
