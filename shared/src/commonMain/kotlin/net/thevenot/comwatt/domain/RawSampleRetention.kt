package net.thevenot.comwatt.domain

import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.MeasureKind
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Comwatt only keeps raw (~2 minute) samples for roughly two months. Beyond that the API answers
 * `{"timestamps":[],"values":[]}` for [AggregationLevel.NONE] while the same range still returns
 * data at [AggregationLevel.HOUR]. Measured cutoff was between 53 and 61 days, hence the margin.
 */
internal val RAW_SAMPLE_RETENTION: Duration = 45.days

/** How a time series has to be asked for: bucket size plus the quantity being measured. */
internal data class SeriesResolution(
    val aggregationLevel: AggregationLevel,
    val measureKind: MeasureKind
)

/**
 * Falls back to hourly buckets when [endTime] is older than the raw-sample retention window,
 * otherwise the chart and its statistics come back empty.
 *
 * [MeasureKind.FLOW] switches to [MeasureKind.QUANTITY] at the same time: the API *sums* the raw
 * samples of a bucket instead of averaging them, so hourly FLOW is about 30x the real power, while
 * hourly QUANTITY (Wh per hour) is numerically the mean power in watts. Resolutions that are
 * already aggregated are returned unchanged.
 */
internal fun SeriesResolution.coarsenedIfRawSamplesExpired(
    endTime: Instant,
    now: Instant
): SeriesResolution =
    if (aggregationLevel == AggregationLevel.NONE && endTime < now - RAW_SAMPLE_RETENTION) {
        SeriesResolution(
            aggregationLevel = AggregationLevel.HOUR,
            measureKind = if (measureKind == MeasureKind.FLOW) {
                MeasureKind.QUANTITY
            } else {
                measureKind
            }
        )
    } else {
        this
    }
