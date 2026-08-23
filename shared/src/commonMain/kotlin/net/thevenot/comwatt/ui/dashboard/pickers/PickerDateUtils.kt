package net.thevenot.comwatt.ui.dashboard.pickers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Material 3 `DatePicker` exposes and expects UTC midnight millis, so every conversion between
 * picker millis and calendar dates must go through [TimeZone.UTC]. Using the system zone shifts the
 * day whenever the local offset is negative.
 */
internal fun LocalDate.toDatePickerMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun datePickerMillisToLocalDate(utcTimeMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(TimeZone.UTC).date

internal fun isSelectableDatePickerMillis(utcTimeMillis: Long, currentDate: LocalDate): Boolean =
    datePickerMillisToLocalDate(utcTimeMillis) <= currentDate

/**
 * Number of whole days between [selectedDate] and [currentDate], i.e. the day offset the dashboard
 * ranges are built from. Must not be derived from `LocalDate.minus(LocalDate)`, which returns a
 * `DatePeriod` whose `days` component only holds the leftover days after whole months.
 */
internal fun dayOffsetBetween(selectedDate: LocalDate, currentDate: LocalDate): Int =
    selectedDate.daysUntil(currentDate)
