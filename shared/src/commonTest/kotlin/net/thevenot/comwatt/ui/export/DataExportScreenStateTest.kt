package net.thevenot.comwatt.ui.export

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataExportScreenStateTest {
    private val today = LocalDate(2026, 8, 24)

    @Test
    fun presetRangeEndsTodayAndSpansItsDayCount() {
        val range = resolveRange(ExportRangePreset.LAST_7_DAYS, null, null, today)

        assertEquals(LocalDate(2026, 8, 17), range?.start)
        assertEquals(today, range?.endInclusive)
    }

    @Test
    fun yearPresetGoesBackThreeHundredAndSixtyFiveDays() {
        val range = resolveRange(ExportRangePreset.LAST_YEAR, null, null, today)

        assertEquals(LocalDate(2025, 8, 24), range?.start)
    }

    @Test
    fun customRangeUsesTheSuppliedDates() {
        val range = resolveRange(
            ExportRangePreset.CUSTOM,
            LocalDate(2026, 1, 1),
            LocalDate(2026, 2, 1),
            today
        )

        assertEquals(LocalDate(2026, 1, 1), range?.start)
        assertEquals(LocalDate(2026, 2, 1), range?.endInclusive)
    }

    @Test
    fun customRangeWithoutBothDatesIsUnresolved() {
        assertNull(resolveRange(ExportRangePreset.CUSTOM, LocalDate(2026, 1, 1), null, today))
        assertNull(resolveRange(ExportRangePreset.CUSTOM, null, LocalDate(2026, 2, 1), today))
    }

    @Test
    fun invertedCustomRangeIsUnresolved() {
        val range = resolveRange(
            ExportRangePreset.CUSTOM,
            LocalDate(2026, 2, 1),
            LocalDate(2026, 1, 1),
            today
        )

        assertNull(range)
    }

    @Test
    fun rowEstimateIsOneRowPerHourInclusiveOfBothDays() {
        val range = resolveRange(ExportRangePreset.LAST_7_DAYS, null, null, today)!!

        assertEquals(8 * 24, estimatedRowCount(range))
    }

    @Test
    fun fetchingProgressIsReportedAsCompletedOfTotal() {
        val status = ExportStatus.Fetching(completed = 6, total = 14)

        assertEquals(6, status.completed)
        assertEquals(14, status.total)
    }

    @Test
    fun defaultStateIsAYearAndIdle() {
        val state = DataExportScreenState()

        assertEquals(ExportRangePreset.LAST_YEAR, state.preset)
        assertEquals(ExportStatus.Idle, state.status)
    }
}
