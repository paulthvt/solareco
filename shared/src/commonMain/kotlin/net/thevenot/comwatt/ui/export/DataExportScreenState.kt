package net.thevenot.comwatt.ui.export

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus

enum class ExportRangePreset(val days: Int?) {
    LAST_7_DAYS(7),
    LAST_30_DAYS(30),
    LAST_3_MONTHS(90),
    LAST_YEAR(365),
    CUSTOM(null)
}

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data class Fetching(val completed: Int, val total: Int) : ExportStatus
    data object Writing : ExportStatus
    data class Saved(val fileName: String) : ExportStatus
    data class Failed(val message: String) : ExportStatus
    data object NoData : ExportStatus
}

data class DataExportScreenState(
    val preset: ExportRangePreset = ExportRangePreset.LAST_YEAR,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
    val status: ExportStatus = ExportStatus.Idle
) {
    val isExporting: Boolean
        get() = status is ExportStatus.Fetching || status is ExportStatus.Writing
}

/** Null when the range cannot be exported yet: an incomplete or inverted custom selection. */
fun resolveRange(
    preset: ExportRangePreset,
    customStart: LocalDate?,
    customEnd: LocalDate?,
    today: LocalDate
): ClosedRange<LocalDate>? {
    val days = preset.days
    if (days != null) return today.minus(DatePeriod(days = days))..today
    if (customStart == null || customEnd == null) return null
    if (customStart > customEnd) return null
    return customStart..customEnd
}

/** Hourly buckets, both endpoints inclusive. Shown before export so a year is not a surprise. */
fun estimatedRowCount(range: ClosedRange<LocalDate>): Int =
    (range.start.daysUntil(range.endInclusive) + 1) * 24
