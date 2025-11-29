package net.thevenot.comwatt.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Android implementation of ChartImageGenerator
 * Uses Android Canvas API to draw charts similar to Vico style
 */
class AndroidChartImageGenerator : ChartImageGenerator {

    private val logger = Logger.withTag("AndroidChartImageGenerator")

    override suspend fun generateChartImage(
        data: WidgetConsumptionData,
        widthPx: Int,
        heightPx: Int,
        isDarkMode: Boolean
    ): ByteArray? = withContext(Dispatchers.Default) {
        try {
            if (data.consumptions.isEmpty()) {
                logger.w { "No consumption data to render" }
                return@withContext null
            }

            // Create bitmap
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Setup colors
            val backgroundColor = if (isDarkMode) 0xFF1E1E1E.toInt() else 0xFFFFFFFF.toInt()
            val lineColor = if (isDarkMode) 0xFF4CAF50.toInt() else 0xFF2E7D32.toInt()
            val gridColor = if (isDarkMode) 0x33FFFFFF else 0x1F000000
            val textColor = if (isDarkMode) 0xFFBBBBBB.toInt() else 0xFF666666.toInt()

            // Clear background
            canvas.drawColor(backgroundColor)

            // Setup paint objects
            val linePaint = Paint().apply {
                color = lineColor
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val fillPaint = Paint().apply {
                color = lineColor
                alpha = 40 // Semi-transparent
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val gridPaint = Paint().apply {
                color = gridColor
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }

            // Chart bounds (with padding)
            val paddingLeft = 50f
            val paddingRight = 20f
            val paddingTop = 20f
            val paddingBottom = 30f

            val chartLeft = paddingLeft
            val chartTop = paddingTop
            val chartRight = widthPx - paddingRight
            val chartBottom = heightPx - paddingBottom
            val chartWidth = chartRight - chartLeft
            val chartHeight = chartBottom - chartTop

            // Prepare data
            val consumptions = data.consumptions.takeLast(60) // Last 60 data points
            val maxValue = max(data.maxConsumption, consumptions.maxOrNull() ?: 1.0)
            val minValue = 0.0
            val range = maxValue - minValue

            if (range <= 0) {
                logger.w { "Invalid data range" }
                return@withContext null
            }

            // Draw grid lines
            val gridLineCount = 4
            for (i in 0..gridLineCount) {
                val y = chartBottom - (i.toFloat() / gridLineCount) * chartHeight
                canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            }

            // Build chart path
            val linePath = Path()
            val fillPath = Path()

            val stepX = chartWidth / max(1, consumptions.size - 1)

            consumptions.forEachIndexed { index, value ->
                val x = chartLeft + index * stepX
                val normalizedValue = ((value - minValue) / range).coerceIn(0.0, 1.0)
                val y = chartBottom - (normalizedValue * chartHeight).toFloat()

                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, chartBottom)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            // Close fill path
            if (consumptions.isNotEmpty()) {
                val lastX = chartLeft + (consumptions.size - 1) * stepX
                fillPath.lineTo(lastX, chartBottom)
                fillPath.close()
            }

            // Draw fill first, then line
            canvas.drawPath(fillPath, fillPaint)
            canvas.drawPath(linePath, linePaint)

            // Draw axis labels
            val textPaint = Paint().apply {
                color = textColor
                textSize = 22f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }

            // Y-axis labels
            for (i in 0..gridLineCount) {
                val value = minValue + (i.toFloat() / gridLineCount) * range
                val y = chartBottom - (i.toFloat() / gridLineCount) * chartHeight
                val label = if (value >= 1000) {
                    "${(value / 1000).toInt()}k"
                } else {
                    "${value.toInt()}"
                }
                canvas.drawText(label, chartLeft - 10f, y + 7f, textPaint)
            }

            // Convert bitmap to PNG bytes
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val bytes = outputStream.toByteArray()

            // Cleanup
            bitmap.recycle()

            logger.d { "Chart image generated: ${bytes.size} bytes" }
            bytes

        } catch (e: Exception) {
            logger.e(e) { "Failed to generate chart image" }
            null
        }
    }
}

/**
 * Android implementation of chart image generator factory
 */
actual fun createChartImageGenerator(): ChartImageGenerator {
    return AndroidChartImageGenerator()
}
