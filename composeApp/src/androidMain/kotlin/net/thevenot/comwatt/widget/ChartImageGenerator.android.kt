package net.thevenot.comwatt.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Android implementation of ChartImageGenerator
 * Uses Android Canvas API to draw charts similar to Vico style
 *
 * Power scheme colors:
 * - Production: Green (#43A047 light, #66BB6A dark)
 * - Consumption: Amber (#FF8F00 light, #FFB300 dark)
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
            if (data.consumptions.isEmpty() && data.productions.isEmpty()) {
                logger.w { "No data to render" }
                return@withContext null
            }

            // Create bitmap
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Setup colors using power scheme colors
            // Production: Green
            val productionColor = if (isDarkMode) 0xFF66BB6A.toInt() else 0xFF43A047.toInt()
            // Consumption: Amber/Orange
            val consumptionColor = if (isDarkMode) 0xFFFFB300.toInt() else 0xFFFF8F00.toInt()
            val gridColor = if (isDarkMode) 0x33FFFFFF else 0x1F000000
            val textColor = if (isDarkMode) 0xFFBBBBBB.toInt() else 0xFF666666.toInt()

            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val gridPaint = Paint().apply {
                color = gridColor
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }

            // Chart bounds (with padding)
            val paddingLeft = 40f
            val paddingRight = 16f
            val paddingTop = 16f
            val paddingBottom = 24f

            val chartLeft = paddingLeft
            val chartTop = paddingTop
            val chartRight = widthPx - paddingRight
            val chartBottom = heightPx - paddingBottom
            val chartWidth = chartRight - chartLeft
            val chartHeight = chartBottom - chartTop

            // Prepare data
            val consumptions = data.consumptions.takeLast(60)
            val productions = data.productions.takeLast(60)
            val maxDataPoints = max(consumptions.size, productions.size)

            val maxValue = max(
                max(data.maxConsumption, consumptions.maxOrNull() ?: 1.0),
                max(data.maxProduction, productions.maxOrNull() ?: 1.0)
            )
            val minValue = 0.0
            val range = if (maxValue - minValue <= 0) 1.0 else maxValue - minValue

            // Draw grid lines
            val gridLineCount = 4
            for (i in 0..gridLineCount) {
                val y = chartBottom - (i.toFloat() / gridLineCount) * chartHeight
                canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            }

            val stepX = if (maxDataPoints > 1) chartWidth / (maxDataPoints - 1) else chartWidth

            // Draw production line first (so consumption appears on top)
            if (productions.isNotEmpty()) {
                drawLine(
                    canvas = canvas,
                    data = productions,
                    color = productionColor,
                    chartLeft = chartLeft,
                    chartBottom = chartBottom,
                    chartHeight = chartHeight,
                    stepX = stepX,
                    minValue = minValue,
                    range = range
                )
            }

            // Draw consumption line
            if (consumptions.isNotEmpty()) {
                drawLine(
                    canvas = canvas,
                    data = consumptions,
                    color = consumptionColor,
                    chartLeft = chartLeft,
                    chartBottom = chartBottom,
                    chartHeight = chartHeight,
                    stepX = stepX,
                    minValue = minValue,
                    range = range
                )
            }

            // Draw axis labels
            val textPaint = Paint().apply {
                color = textColor
                textSize = 20f
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
                canvas.drawText(label, chartLeft - 6f, y + 6f, textPaint)
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

    private fun drawLine(
        canvas: Canvas,
        data: List<Double>,
        color: Int,
        chartLeft: Float,
        chartBottom: Float,
        chartHeight: Float,
        stepX: Float,
        minValue: Double,
        range: Double
    ) {
        val linePaint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val fillPaint = Paint().apply {
            this.color = color
            alpha = 40
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val linePath = Path()
        val fillPath = Path()

        data.forEachIndexed { index, value ->
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
        if (data.isNotEmpty()) {
            val lastX = chartLeft + (data.size - 1) * stepX
            fillPath.lineTo(lastX, chartBottom)
            fillPath.close()
        }

        // Draw fill first, then line
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
    }
}

/**
 * Android implementation of chart image generator factory
 */
actual fun createChartImageGenerator(): ChartImageGenerator {
    return AndroidChartImageGenerator()
}
