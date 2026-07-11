package net.thevenot.comwatt.model.savings

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

sealed interface SavingsPeriod {
    data object Today : SavingsPeriod
    data object ThisMonth : SavingsPeriod
    data object ThisYear : SavingsPeriod
    data class Custom(val start: Instant, val end: Instant) : SavingsPeriod
}

/** Returns (startInclusive, endExclusive) instants for the period. */
fun SavingsPeriod.toRange(now: Instant, zone: TimeZone): Pair<Instant, Instant> {
    val today = now.toLocalDateTime(zone).date
    return when (this) {
        SavingsPeriod.Today -> today.atStartOfDayIn(zone) to now
        SavingsPeriod.ThisMonth ->
            LocalDate(today.year, today.monthNumber, 1).atStartOfDayIn(zone) to now
        SavingsPeriod.ThisYear ->
            LocalDate(today.year, 1, 1).atStartOfDayIn(zone) to now
        is SavingsPeriod.Custom -> start to end
    }
}
