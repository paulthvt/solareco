package net.thevenot.comwatt.widget

import kotlin.test.Test
import kotlin.test.assertEquals

class ChartAxisTest {
    @Test
    fun axisValuesUsePowerOfTenSteps() {
        val testCases = listOf(
            3532.0 to listOf(0.0, 1000.0, 2000.0, 3000.0),
            2308.0 to listOf(0.0, 1000.0, 2000.0),
            900.0 to listOf(0.0, 100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0),
            999.0 to listOf(0.0, 100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0),
            12.0 to listOf(0.0, 10.0),
            1.0 to listOf(0.0, 1.0)
        )

        for ((maxValue, expected) in testCases) {
            assertEquals(expected, ChartAxis.axisValues(maxValue), "Failed for max: $maxValue")
        }
    }

    @Test
    fun axisValuesIncludeExactMaxWhenItIsAMultipleOfTheStep() {
        assertEquals(listOf(0.0, 1000.0, 2000.0, 3000.0), ChartAxis.axisValues(3000.0))
    }

    @Test
    fun axisValuesNeverExceedMax() {
        for (maxValue in listOf(1.0, 12.0, 900.0, 2308.0, 3532.0, 10000.0)) {
            val values = ChartAxis.axisValues(maxValue)
            assertEquals(
                emptyList(),
                values.filter { it > maxValue },
                "Ticks above max for $maxValue"
            )
        }
    }

    @Test
    fun axisValuesHandleNonPositiveMax() {
        assertEquals(listOf(0.0), ChartAxis.axisValues(0.0))
        assertEquals(listOf(0.0), ChartAxis.axisValues(-5.0))
    }

    @Test
    fun formatLabelAbbreviatesThousands() {
        val testCases = listOf(
            0.0 to "0",
            883.0 to "883",
            1000.0 to "1k",
            2000.0 to "2k",
            3000.0 to "3k"
        )

        for ((value, expected) in testCases) {
            assertEquals(expected, ChartAxis.formatLabel(value), "Failed for value: $value")
        }
    }
}
