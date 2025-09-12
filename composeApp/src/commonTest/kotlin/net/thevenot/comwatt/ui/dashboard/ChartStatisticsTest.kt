package net.thevenot.comwatt.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import kotlinx.datetime.Instant
import net.thevenot.comwatt.domain.model.TimeSeries
import net.thevenot.comwatt.domain.model.TimeSeriesTitle
import net.thevenot.comwatt.domain.model.TimeSeriesType
import kotlin.test.Test
import kotlin.test.assertEquals

class ChartStatisticsTest {

    @Test
    fun `computeFromTimeSeries with empty values should return zeros`() {
        // Given
        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Empty Series", Icons.Default.Star),
            type = TimeSeriesType.CONSUMPTION,
            values = emptyMap()
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(ChartStatistics(0.0, 0.0, 0.0, 0.0, false), result)
    }

    @Test
    fun `computeFromTimeSeries with multiple values should compute correct statistics`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)
        val instant3 = Instant.fromEpochSeconds(3000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Test Series", Icons.Default.Star),
            type = TimeSeriesType.PRODUCTION,
            values = mapOf(
                instant1 to 10.0f,
                instant2 to 20.0f,
                instant3 to 30.0f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(10.0, result.min, 0.001)
        assertEquals(30.0, result.max, 0.001)
        assertEquals(20.0, result.average, 0.001)
        assertEquals(60.0, result.sum, 0.001)
        assertEquals(false, result.isLoading)
    }

    @Test
    fun `computeFromTimeSeries with single value should handle correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Single Value Series", Icons.Default.Star),
            type = TimeSeriesType.INJECTION,
            values = mapOf(instant1 to 42.5f)
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(42.5, result.min, 0.001)
        assertEquals(42.5, result.max, 0.001)
        assertEquals(42.5, result.average, 0.001)
        assertEquals(42.5, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries with negative values should compute correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)
        val instant3 = Instant.fromEpochSeconds(3000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Negative Series", Icons.Default.Star),
            type = TimeSeriesType.WITHDRAWAL,
            values = mapOf(
                instant1 to -10.0f,
                instant2 to 5.0f,
                instant3 to -2.0f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(-10.0, result.min, 0.001)
        assertEquals(5.0, result.max, 0.001)
        assertEquals(-2.333, result.average, 0.001)
        assertEquals(-7.0, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries with all negative values should compute correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)
        val instant3 = Instant.fromEpochSeconds(3000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("All Negative Series", Icons.Default.Star),
            type = TimeSeriesType.CONSUMPTION,
            values = mapOf(
                instant1 to -5.0f,
                instant2 to -15.0f,
                instant3 to -10.0f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(-15.0, result.min, 0.001)
        assertEquals(-5.0, result.max, 0.001)
        assertEquals(-10.0, result.average, 0.001)
        assertEquals(-30.0, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries with decimal values should compute correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)
        val instant3 = Instant.fromEpochSeconds(3000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Decimal Series", Icons.Default.Star),
            type = TimeSeriesType.PRODUCTION,
            values = mapOf(
                instant1 to 3.14f,
                instant2 to 2.71f,
                instant3 to 1.41f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(1.41, result.min, 0.001)
        assertEquals(3.14, result.max, 0.001)
        assertEquals(2.42, result.average, 0.001)
        assertEquals(7.26, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries with zero values should handle correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)
        val instant3 = Instant.fromEpochSeconds(3000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Zero Series", Icons.Default.Star),
            type = TimeSeriesType.CONSUMPTION,
            values = mapOf(
                instant1 to 0.0f,
                instant2 to 0.0f,
                instant3 to 0.0f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(0.0, result.min, 0.001)
        assertEquals(0.0, result.max, 0.001)
        assertEquals(0.0, result.average, 0.001)
        assertEquals(0.0, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries with mixed zero and non-zero values should compute correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)
        val instant3 = Instant.fromEpochSeconds(3000)
        val instant4 = Instant.fromEpochSeconds(4000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Mixed Zero Series", Icons.Default.Star),
            type = TimeSeriesType.PRODUCTION,
            values = mapOf(
                instant1 to 0.0f,
                instant2 to 10.0f,
                instant3 to 0.0f,
                instant4 to 20.0f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(0.0, result.min, 0.001)
        assertEquals(20.0, result.max, 0.001)
        assertEquals(7.5, result.average, 0.001)
        assertEquals(30.0, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries with very small values should handle correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Small Values Series", Icons.Default.Star),
            type = TimeSeriesType.INJECTION,
            values = mapOf(
                instant1 to 0.001f,
                instant2 to 0.002f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(0.001, result.min, 0.0001)
        assertEquals(0.002, result.max, 0.0001)
        assertEquals(0.0015, result.average, 0.0001)
        assertEquals(0.003, result.sum, 0.0001)
    }

    @Test
    fun `computeFromTimeSeries with very large values should handle correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)
        val instant3 = Instant.fromEpochSeconds(3000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Large Values Series", Icons.Default.Star),
            type = TimeSeriesType.PRODUCTION,
            values = mapOf(
                instant1 to 1000000.0f,
                instant2 to 2000000.0f,
                instant3 to 3000000.0f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(1000000.0, result.min, 0.001)
        assertEquals(3000000.0, result.max, 0.001)
        assertEquals(2000000.0, result.average, 0.001)
        assertEquals(6000000.0, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries with duplicate values should compute correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)
        val instant3 = Instant.fromEpochSeconds(3000)
        val instant4 = Instant.fromEpochSeconds(4000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Duplicate Values Series", Icons.Default.Star),
            type = TimeSeriesType.WITHDRAWAL,
            values = mapOf(
                instant1 to 15.0f,
                instant2 to 15.0f,
                instant3 to 15.0f,
                instant4 to 15.0f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(15.0, result.min, 0.001)
        assertEquals(15.0, result.max, 0.001)
        assertEquals(15.0, result.average, 0.001)
        assertEquals(60.0, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries with extreme range values should compute correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)
        val instant3 = Instant.fromEpochSeconds(3000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Extreme Range Series", Icons.Default.Star),
            type = TimeSeriesType.CONSUMPTION,
            values = mapOf(
                instant1 to -1000.0f,
                instant2 to 0.5f,
                instant3 to 2000.0f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(-1000.0, result.min, 0.001)
        assertEquals(2000.0, result.max, 0.001)
        assertEquals(333.5, result.average, 0.001)
        assertEquals(1000.5, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries with single zero value should handle correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Single Zero Series", Icons.Default.Star),
            type = TimeSeriesType.PRODUCTION,
            values = mapOf(instant1 to 0.0f)
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(0.0, result.min, 0.001)
        assertEquals(0.0, result.max, 0.001)
        assertEquals(0.0, result.average, 0.001)
        assertEquals(0.0, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries with single negative value should handle correctly`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Single Negative Series", Icons.Default.Star),
            type = TimeSeriesType.WITHDRAWAL,
            values = mapOf(instant1 to -25.5f)
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(-25.5, result.min, 0.001)
        assertEquals(-25.5, result.max, 0.001)
        assertEquals(-25.5, result.average, 0.001)
        assertEquals(-25.5, result.sum, 0.001)
    }

    @Test
    fun `computeFromTimeSeries should always set isLoading to false`() {
        // Given
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)

        val timeSeries = TimeSeries(
            title = TimeSeriesTitle("Loading Test Series", Icons.Default.Star),
            type = TimeSeriesType.PRODUCTION,
            values = mapOf(
                instant1 to 100.0f,
                instant2 to 200.0f
            )
        )

        // When
        val result = ChartStatistics.computeFromTimeSeries(timeSeries)

        // Then
        assertEquals(false, result.isLoading)
    }
}