package net.thevenot.comwatt.widget

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Y axis helpers shared by the widget chart renderers.
 *
 * These mirror the in-app chart (`DashboardScreen`), which fixes the Y range to
 * `0..maxValue` and places labels on a "nice step" of 10^floor(log10(maxValue)).
 * Keeping the same rule here means a value of 3532 W is labelled 0 / 1k / 2k / 3k
 * in both places, with the peak drawn above the 3k line.
 */
object ChartAxis {
    /** Tick values from 0 up to (and not beyond) [maxValue], on a power-of-ten step. */
    fun axisValues(maxValue: Double): List<Double> {
        if (maxValue <= 0.0 || !maxValue.isFinite()) return listOf(0.0)

        val step = 10.0.pow(floor(log10(maxValue)))
        if (step <= 0.0 || !step.isFinite()) return listOf(0.0, maxValue)

        val values = mutableListOf<Double>()
        var value = 0.0
        while (value <= maxValue) {
            values.add(value)
            value += step
        }
        return values
    }

    /** Formats a tick value, abbreviating thousands the way the app's axis does. */
    fun formatLabel(value: Double): String =
        if (value >= 1000) "${(value / 1000).toInt()}k" else "${value.toInt()}"
}
