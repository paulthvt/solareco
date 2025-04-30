package net.thevenot.comwatt.domain.utils
import kotlinx.datetime.Instant
import net.thevenot.comwatt.domain.model.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeSeriesDownsamplerTest {
    @Test
    fun testDownsampleEmptyData() {
        val emptyData = mapOf<Instant, Float>()
        val result = TimeSeriesDownsampler.downsample(emptyData, TimeUnit.DAY)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testDownsampleSingleDataPoint() {
        val instant = Instant.fromEpochSeconds(1000)
        val singleData = mapOf(instant to 10.0f)

        val result = TimeSeriesDownsampler.downsample(singleData, TimeUnit.DAY)

        assertEquals(1, result.size)
        assertEquals(10.0f, result[instant])
    }

    @Test
    fun testDownsampleTwoDataPoints() {
        val instant1 = Instant.fromEpochSeconds(1000)
        val instant2 = Instant.fromEpochSeconds(2000)
        val twoData = mapOf(
            instant1 to 10.0f,
            instant2 to 20.0f
        )

        val result = TimeSeriesDownsampler.downsample(twoData, TimeUnit.DAY)

        assertEquals(2, result.size)
        assertEquals(10.0f, result[instant1])
        assertEquals(20.0f, result[instant2])
    }

    @Test
    fun testDownsampleHourTimeUnit() {
        val data = createTestData(200)

        val result = TimeSeriesDownsampler.downsample(data, TimeUnit.HOUR)

        assertEquals(119, result.size)
        val firstKey = data.keys.first()
        val lastKey = data.keys.last()
        assertEquals(data[firstKey], result[firstKey])
        assertEquals(data[lastKey], result[lastKey])
    }

    @Test
    fun testDownsampleDayTimeUnit() {
        val data = createTestData(300)

        val result = TimeSeriesDownsampler.downsample(data, TimeUnit.DAY)

        assertEquals(143, result.size)
        val firstKey = data.keys.first()
        val lastKey = data.keys.last()
        assertEquals(data[firstKey], result[firstKey])
        assertEquals(data[lastKey], result[lastKey])
    }

    @Test
    fun testDownsampleWeekTimeUnit() {
        val data = createTestData(400)

        val result = TimeSeriesDownsampler.downsample(data, TimeUnit.WEEK)

        assertEquals(167, result.size)
        val firstKey = data.keys.first()
        val lastKey = data.keys.last()
        assertEquals(data[firstKey], result[firstKey])
        assertEquals(data[lastKey], result[lastKey])
    }

    @Test
    fun testDownsampleFewDataPoints() {
        val data = createTestData(50)

        val result = TimeSeriesDownsampler.downsample(data, TimeUnit.DAY)

        assertEquals(data.size, result.size)
        assertEquals(data, result)
    }

    @Test
    fun testDownsampleWithIrregularTimeIntervals() {
        val irregularData = mapOf(
            Instant.fromEpochSeconds(1000) to 10.0f,
            Instant.fromEpochSeconds(1005) to 15.0f,
            Instant.fromEpochSeconds(1500) to 20.0f, // gap
            Instant.fromEpochSeconds(1501) to 25.0f,
            Instant.fromEpochSeconds(1502) to 30.0f,
            Instant.fromEpochSeconds(2000) to 35.0f, // gap
            Instant.fromEpochSeconds(3000) to 40.0f  // large gap
        )

        val result = TimeSeriesDownsampler.downsample(irregularData, TimeUnit.HOUR)

        val firstKey = irregularData.keys.first()
        val lastKey = irregularData.keys.last()
        assertEquals(irregularData[firstKey], result[firstKey])
        assertEquals(irregularData[lastKey], result[lastKey])
    }

    @Test
    fun testDownsampleWithNegativeValues() {
        val data = mapOf(
            Instant.fromEpochSeconds(1000) to -10.0f,
            Instant.fromEpochSeconds(1100) to -5.0f,
            Instant.fromEpochSeconds(1200) to 0.0f,
            Instant.fromEpochSeconds(1300) to 5.0f,
            Instant.fromEpochSeconds(1400) to 10.0f
        )

        val result = TimeSeriesDownsampler.downsample(data, TimeUnit.HOUR)

        val firstKey = data.keys.first()
        val lastKey = data.keys.last()
        assertEquals(data[firstKey], result[firstKey])
        assertEquals(data[lastKey], result[lastKey])
    }

    @Test
    fun testDownsampleZeroValues() {
        val data = mapOf(
            Instant.fromEpochSeconds(1000) to 0.0f,
            Instant.fromEpochSeconds(1100) to 0.0f,
            Instant.fromEpochSeconds(1200) to 0.0f,
            Instant.fromEpochSeconds(1300) to 0.0f,
            Instant.fromEpochSeconds(1400) to 0.0f
        )

        val result = TimeSeriesDownsampler.downsample(data, TimeUnit.HOUR)

        val firstKey = data.keys.first()
        val lastKey = data.keys.last()
        assertEquals(0.0f, result[firstKey])
        assertEquals(0.0f, result[lastKey])
    }

    private fun createTestData(size: Int): Map<Instant, Float> {
        val data = mutableMapOf<Instant, Float>()
        for (i in 0 until size) {
            data[Instant.fromEpochSeconds(1000L + i * 60L)] = i.toFloat() // 1 minute interval
        }
        return data
    }
}