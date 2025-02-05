package net.thevenot.comwatt.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.github.koalaplot.core.xygraph.AxisModel
import io.github.koalaplot.core.xygraph.TickValues
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Composable
fun rememberTimeAxisModel(
    minimumMajorTickSpacing: Dp = 50.dp,
    tickInterval: Duration = 4.hours,
    rangeProvider: () -> ClosedRange<Instant>,
): AxisModel<Instant> = remember(
    minimumMajorTickSpacing,
    tickInterval,
    rangeProvider
) {
    TimeAxisModel(minimumMajorTickSpacing, tickInterval, rangeProvider)
}

class TimeAxisModel(
    private val minimumMajorTickSpacing: Dp = 50.dp,
    private val tickInterval: Duration = 4.hours,
    private val rangeProvider: () -> ClosedRange<Instant>,
) : AxisModel<Instant> {

    override fun computeTickValues(axisLength: Dp): TickValues<Instant> {
        val currentRange = rangeProvider()
        Napier.d { "TimeAxisModel initial: $currentRange" }
        val rangeLength = currentRange.endInclusive - currentRange.start
        val numTicks = floor(axisLength / minimumMajorTickSpacing).toInt()
        val tickSpacing = (rangeLength / numTicks).coerceAtLeast(tickInterval)

        val startDateTime = currentRange.start.toLocalDateTime(TimeZone.currentSystemDefault())
        val midnight = startDateTime.date.atStartOfDayIn(TimeZone.currentSystemDefault())

        val majorTickValues = mutableListOf<Instant>()
        val currentTick = if (currentRange.start < midnight) {
            midnight
        } else {
            val nextTickHour =
                ((startDateTime.hour / tickInterval.inWholeHours) + 1) * tickInterval.inWholeHours
            midnight.plus(nextTickHour.hours)
        }
        majorTickValues.add(currentTick)

        generateSequence(currentTick + tickSpacing) {
            it + tickSpacing
        }.takeWhile { it <= currentRange.endInclusive }.forEach {
            majorTickValues.add(it)
        }

        Napier.d { "TimeAxisModel: $majorTickValues" }

        return object : TickValues<Instant> {
            override val majorTickValues: List<Instant> = majorTickValues
            override val minorTickValues: List<Instant> = emptyList()
        }
    }

    override fun computeOffset(point: Instant): Float {
        val currentRange = rangeProvider()
        return ((point - currentRange.start) / (currentRange.endInclusive - currentRange.start)).toFloat()
    }
}
