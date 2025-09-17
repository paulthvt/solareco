package net.thevenot.comwatt.domain.utils

import net.thevenot.comwatt.domain.model.TimeUnit
import kotlin.math.abs
import kotlin.time.Instant

object TimeSeriesDownsampler {
    fun downsample(
        data: Map<Instant, Float>,
        timeUnit: TimeUnit
    ): Map<Instant, Float> {
        val targetPointCount = when (timeUnit) {
            TimeUnit.HOUR -> 120  // hour - more detail
            TimeUnit.DAY -> 144   // day - 24*6 points (every 10 min)
            TimeUnit.WEEK -> 168   // week - 7*24 points (every 1 hours)
            else -> 150
        }

        return downsampleTimeSeries(data, targetPointCount)
    }

    /**
     * Downsamples time series data using LTTB algorithm that preserves the visual shape
     * of the chart while reducing the number of points.
     */
    private fun downsampleTimeSeries(
        data: Map<Instant, Float>,
        targetPointCount: Int
    ): Map<Instant, Float> {
        if (data.size <= targetPointCount || data.size <= 2) return data

        val sorted = data.entries.sortedBy { it.key }
        val result = mutableMapOf<Instant, Float>()

        result[sorted.first().key] = sorted.first().value

        val bucketSize = (sorted.size - 2) / (targetPointCount - 2)

        for (i in 0 until targetPointCount - 2) {
            val startIdx = i * bucketSize
            val endIdx = ((i + 1) * bucketSize).coerceAtMost(sorted.size - 1)

            val bucketData = sorted.subList(startIdx, endIdx)

            var maxArea = 0f
            var maxAreaIdx = startIdx

            val prevPoint = if (i > 0) {
                val lastKey = result.keys.last()
                sorted.first { it.key == lastKey }
            } else {
                sorted[0]
            }
            val nextBucketMidpoint = sorted[(endIdx + bucketSize).coerceAtMost(sorted.size - 1)]

            bucketData.forEachIndexed { idx, point ->
                val area = calculateTriangleArea(prevPoint, point, nextBucketMidpoint)
                if (area > maxArea) {
                    maxArea = area
                    maxAreaIdx = startIdx + idx
                }
            }

            result[sorted[maxAreaIdx].key] = sorted[maxAreaIdx].value
        }

        result[sorted.last().key] = sorted.last().value
        return result
    }

    private fun calculateTriangleArea(
        a: Map.Entry<Instant, Float>,
        b: Map.Entry<Instant, Float>,
        c: Map.Entry<Instant, Float>
    ): Float {
        val aX = a.key.epochSeconds.toFloat()
        val bX = b.key.epochSeconds.toFloat()
        val cX = c.key.epochSeconds.toFloat()

        return 0.5f * abs((aX * (b.value - c.value) + bX * (c.value - a.value) + cX * (a.value - b.value)))
    }
}