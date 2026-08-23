package net.thevenot.comwatt.domain

import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.MeasureKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class RawSampleRetentionTest {
    private val now = Instant.parse("2026-08-23T19:58:47Z")
    private val rawFlow = SeriesResolution(AggregationLevel.NONE, MeasureKind.FLOW)

    @Test
    fun rawResolutionIsKeptForRecentRanges() {
        assertEquals(rawFlow, rawFlow.coarsenedIfRawSamplesExpired(now - 1.days, now))
    }

    @Test
    fun rawResolutionIsKeptRightBeforeTheRetentionLimit() {
        val endTime = now - RAW_SAMPLE_RETENTION + 1.days

        assertEquals(rawFlow, rawFlow.coarsenedIfRawSamplesExpired(endTime, now))
    }

    @Test
    fun rawFlowBecomesHourlyEnergyPastTheRetentionLimit() {
        val endTime = now - RAW_SAMPLE_RETENTION - 1.days

        assertEquals(
            SeriesResolution(AggregationLevel.HOUR, MeasureKind.QUANTITY),
            rawFlow.coarsenedIfRawSamplesExpired(endTime, now)
        )
    }

    @Test
    fun januaryRangeBecomesHourlyEnergy() {
        val endTime = Instant.parse("2026-01-12T20:58:47Z")

        assertEquals(
            SeriesResolution(AggregationLevel.HOUR, MeasureKind.QUANTITY),
            rawFlow.coarsenedIfRawSamplesExpired(endTime, now)
        )
    }

    @Test
    fun rawEnergyOnlyGetsItsBucketSizeCoarsened() {
        val endTime = Instant.parse("2026-01-12T20:58:47Z")
        val rawQuantity = SeriesResolution(AggregationLevel.NONE, MeasureKind.QUANTITY)

        assertEquals(
            SeriesResolution(AggregationLevel.HOUR, MeasureKind.QUANTITY),
            rawQuantity.coarsenedIfRawSamplesExpired(endTime, now)
        )
    }

    @Test
    fun alreadyAggregatedResolutionsAreLeftUntouched() {
        val endTime = Instant.parse("2026-01-12T20:58:47Z")

        listOf(
            SeriesResolution(AggregationLevel.HOUR, MeasureKind.QUANTITY),
            SeriesResolution(AggregationLevel.DAY, MeasureKind.FLOW),
            SeriesResolution(AggregationLevel.MONTH, MeasureKind.QUANTITY)
        ).forEach { resolution ->
            assertEquals(resolution, resolution.coarsenedIfRawSamplesExpired(endTime, now))
        }
    }
}
