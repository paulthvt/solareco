package net.thevenot.comwatt.ui.dashboard

import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.multiplatform.common.data.ExtraStore
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TimeAlignedItemPlacer(
    private val shiftExtremeLines: Boolean = true,
    private val addExtremeLabelPadding: Boolean = true
) : HorizontalAxis.ItemPlacer {

    companion object {
        val TimeUnitIndexKey = object : ExtraStore.Key<DashboardTimeUnit>() {}
        val RangeDurationKey = object : ExtraStore.Key<Duration>() {}
    }

    override fun getShiftExtremeLines(context: CartesianDrawingContext): Boolean = shiftExtremeLines

    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float
    ): List<Double> {
        val rangeDuration = context.model.extraStore.getOrNull(RangeDurationKey)

        val intervalSeconds = when {
            rangeDuration == null -> {
                val timeUnitIndex =
                    context.model.extraStore.getOrNull(TimeUnitIndexKey) ?: DashboardTimeUnit.HOUR
                when (timeUnitIndex) {
                    DashboardTimeUnit.HOUR -> 15.minutes.inWholeSeconds
                    DashboardTimeUnit.DAY -> 4.hours.inWholeSeconds
                    DashboardTimeUnit.WEEK, DashboardTimeUnit.CUSTOM -> 1.days.inWholeSeconds
                }
            }

            rangeDuration < 15.minutes -> 1.minutes.inWholeSeconds
            rangeDuration < 30.minutes -> 5.minutes.inWholeSeconds
            rangeDuration < 1.hours -> 10.minutes.inWholeSeconds
            rangeDuration < 4.hours -> 15.minutes.inWholeSeconds
            rangeDuration < 1.days -> 4.hours.inWholeSeconds
            rangeDuration < 7.days -> 1.days.inWholeSeconds
            else -> 7.days.inWholeSeconds
        }

        val startTimestamp = visibleXRange.start

        val intervalOffset = startTimestamp % intervalSeconds
        var firstTickPosition = startTimestamp - intervalOffset
        if (firstTickPosition < startTimestamp) {
            firstTickPosition += intervalSeconds
        }

        val values = mutableListOf<Double>()
        val overflowCount = 2

        var position = firstTickPosition - intervalSeconds * overflowCount
        while (position <= visibleXRange.endInclusive + intervalSeconds * overflowCount) {
            if (position >= fullXRange.start && position <= fullXRange.endInclusive) {
                values.add(position)
            }
            position += intervalSeconds
        }

        return values
    }

    override fun getWidthMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>
    ): List<Double> {
        return listOf(
            fullXRange.start,
            fullXRange.start + (fullXRange.endInclusive - fullXRange.start) / 2,
            fullXRange.endInclusive
        )
    }

    override fun getHeightMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float
    ): List<Double> {
        return context.ranges.run { listOf(minX, maxX) }
    }

    override fun getFirstLabelValue(
        context: CartesianMeasuringContext,
        maxLabelWidth: Float
    ): Double? =
        if (addExtremeLabelPadding) context.ranges.minX else null

    override fun getLastLabelValue(
        context: CartesianMeasuringContext,
        maxLabelWidth: Float
    ): Double? =
        if (addExtremeLabelPadding) context.ranges.maxX else null

    override fun getStartLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float
    ): Float {
        val tickSpace = if (shiftExtremeLines) tickThickness else tickThickness / 2
        return (tickSpace - layerDimensions.unscalableStartPadding).coerceAtLeast(0f)
    }

    override fun getEndLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float
    ): Float {
        val tickSpace = if (shiftExtremeLines) tickThickness else tickThickness / 2
        return (tickSpace - layerDimensions.unscalableEndPadding).coerceAtLeast(0f)
    }
}