package net.thevenot.comwatt.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.ui.theme.WidgetColors
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

class AndroidStatisticsImageGenerator : StatisticsImageGenerator {

    override suspend fun generateDonutImage(params: DonutImageParams): ByteArray? =
        withContext(Dispatchers.Default) {
            val size = params.sizePx
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val colors = Colors(params.isDarkMode)
            val primaryColor: Int
            val trackColor: Int
            if (params.isSecondary) {
                primaryColor = colors.consumption
                trackColor = colors.withdrawalsTrack
            } else {
                primaryColor = colors.production
                trackColor = colors.injectionTrack
            }

            val strokeWidth = size * 0.18f
            val padding = strokeWidth / 2f
            val rect = RectF(padding, padding, size - padding, size - padding)

            val trackPaint = Paint().apply {
                color = trackColor
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }
            canvas.drawArc(rect, 0f, 360f, false, trackPaint)

            if (params.percentage != null && params.percentage > 0f) {
                val sweepAngle = params.percentage.coerceIn(0f, 1f) * 360f
                val arcPaint = Paint().apply {
                    color = primaryColor
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                }
                canvas.drawArc(rect, -90f, sweepAngle, false, arcPaint)
            }

            val percentText = if (params.percentage != null) {
                "${(params.percentage * 100).roundToInt()}%"
            } else {
                "N/A"
            }
            val textPaint = Paint().apply {
                color = colors.text
                textSize = size * 0.22f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
            }
            val centerX = size / 2f
            val centerY = size / 2f
            canvas.drawText(percentText, centerX, centerY + textPaint.textSize / 3f, textPaint)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val bytes = outputStream.toByteArray()
            bitmap.recycle()
            bytes
        }

    private class Colors(isDarkMode: Boolean) {
        val production = WidgetColors.productionColor(isDarkMode)
        val consumption = WidgetColors.consumptionColor(isDarkMode)
        val injection = WidgetColors.injectionColor(isDarkMode)
        val withdrawals = WidgetColors.withdrawalsColor(isDarkMode)
        val text = WidgetColors.textColor(isDarkMode)
        val injectionTrack =
            Color.argb(51, Color.red(injection), Color.green(injection), Color.blue(injection))
        val withdrawalsTrack = Color.argb(
            51,
            Color.red(withdrawals),
            Color.green(withdrawals),
            Color.blue(withdrawals),
        )
    }
}

actual fun createStatisticsImageGenerator(): StatisticsImageGenerator =
    AndroidStatisticsImageGenerator()
