package net.thevenot.comwatt.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Shader
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.ui.theme.WidgetColors
import java.io.ByteArrayOutputStream
import kotlin.math.max

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

            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val colors = Colors(isDarkMode)
            val chartBounds = ChartBounds(widthPx.toFloat(), heightPx.toFloat())

            drawGridLines(canvas, chartBounds, colors.grid)

            val consumptions = data.consumptions.takeLast(60)
            val productions = data.productions.takeLast(60)
            val maxDataPoints = max(consumptions.size, productions.size)
            val maxValue = calculateMaxValue(data, consumptions, productions)
            val stepX =
                if (maxDataPoints > 1) chartBounds.width / (maxDataPoints - 1) else chartBounds.width

            if (productions.isNotEmpty()) {
                drawDataLine(canvas, productions, colors.production, chartBounds, stepX, maxValue)
            }
            if (consumptions.isNotEmpty()) {
                drawDataLine(canvas, consumptions, colors.consumption, chartBounds, stepX, maxValue)
            }

            drawYAxisLabels(canvas, chartBounds, maxValue, colors.text)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val bytes = outputStream.toByteArray()
            bitmap.recycle()

            logger.d { "Chart image generated: ${bytes.size} bytes" }
            bytes
        } catch (e: Exception) {
            logger.e(e) { "Failed to generate chart image" }
            null
        }
    }

    private fun calculateMaxValue(
        data: WidgetConsumptionData,
        consumptions: List<Double>,
        productions: List<Double>
    ): Double {
        return max(
            max(data.maxConsumption, consumptions.maxOrNull() ?: 1.0),
            max(data.maxProduction, productions.maxOrNull() ?: 1.0)
        )
    }

    private fun drawGridLines(canvas: Canvas, bounds: ChartBounds, gridColor: Int) {
        val paint = Paint().apply {
            color = gridColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        val gridLineCount = 4
        for (i in 0..gridLineCount) {
            val y = bounds.bottom - (i.toFloat() / gridLineCount) * bounds.height
            canvas.drawLine(bounds.left, y, bounds.right, y, paint)
        }
    }

    private fun drawDataLine(
        canvas: Canvas,
        data: List<Double>,
        color: Int,
        bounds: ChartBounds,
        stepX: Float,
        maxValue: Double
    ) {
        val linePaint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val colorWithAlpha =
            Color.argb(102, Color.red(color), Color.green(color), Color.blue(color))
        val fillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            shader = LinearGradient(
                0f, bounds.top,
                0f, bounds.bottom,
                colorWithAlpha,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }

        val linePath = Path()
        val fillPath = Path()

        data.forEachIndexed { index, value ->
            val x = bounds.left + index * stepX
            val normalizedValue = (value / maxValue).coerceIn(0.0, 1.0)
            val y = bounds.bottom - (normalizedValue * bounds.height).toFloat()

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, bounds.bottom)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        if (data.isNotEmpty()) {
            val lastX = bounds.left + (data.size - 1) * stepX
            fillPath.lineTo(lastX, bounds.bottom)
            fillPath.close()
        }

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
    }

    private fun drawYAxisLabels(
        canvas: Canvas,
        bounds: ChartBounds,
        maxValue: Double,
        textColor: Int
    ) {
        val paint = Paint().apply {
            color = textColor
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val gridLineCount = 4
        for (i in 0..gridLineCount) {
            val value = (i.toFloat() / gridLineCount) * maxValue
            val y = bounds.bottom - (i.toFloat() / gridLineCount) * bounds.height
            val label = if (value >= 1000) "${(value / 1000).toInt()}k" else "${value.toInt()}"
            canvas.drawText(label, bounds.left - 6f, y + 6f, paint)
        }
    }

    private class Colors(isDarkMode: Boolean) {
        val production = WidgetColors.productionColor(isDarkMode)
        val consumption = WidgetColors.consumptionColor(isDarkMode)
        val grid = WidgetColors.gridColor(isDarkMode)
        val text = WidgetColors.textColor(isDarkMode)
    }

    private class ChartBounds(canvasWidth: Float, canvasHeight: Float) {
        private val paddingLeft = 40f
        private val paddingRight = 16f
        private val paddingTop = 16f
        private val paddingBottom = 24f

        val left = paddingLeft
        val top = paddingTop
        val right = canvasWidth - paddingRight
        val bottom = canvasHeight - paddingBottom
        val width = right - left
        val height = bottom - top
    }
}

actual fun createChartImageGenerator(): ChartImageGenerator = AndroidChartImageGenerator()