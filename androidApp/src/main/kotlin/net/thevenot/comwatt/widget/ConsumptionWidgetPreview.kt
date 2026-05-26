package net.thevenot.comwatt.widget

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import kotlinx.coroutines.runBlocking

private const val PREVIEW_CHART_WIDTH = 640
private const val PREVIEW_CHART_HEIGHT = 240

private fun generatePreviewBitmap(data: WidgetConsumptionData): android.graphics.Bitmap? {
    val generator = createChartImageGenerator()
    val bytes = runBlocking {
        generator.generateChartImage(
            data = data,
            widthPx = PREVIEW_CHART_WIDTH,
            heightPx = PREVIEW_CHART_HEIGHT,
            isDarkMode = false,
        )
    }
    return bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 180)
@Composable
fun PreviewConsumptionWidgetWithSolarAvailable() {
    val data = WidgetConsumptionData(
        timestamps = listOf(1L, 2L, 3L, 4L, 5L),
        consumptions = listOf(800.0, 850.0, 900.0, 750.0, 600.0),
        productions = listOf(1200.0, 1300.0, 1400.0, 1100.0, 1500.0),
        lastUpdateTime = System.currentTimeMillis(),
        maxConsumption = 900.0,
        averageConsumption = 780.0,
        maxProduction = 1500.0,
        averageProduction = 1300.0,
    )
    ConsumptionWidgetContent(data, previewBitmap = generatePreviewBitmap(data))
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 180)
@Composable
fun PreviewConsumptionWidgetNoSolarAvailable() {
    val data = WidgetConsumptionData(
        timestamps = listOf(1L, 2L, 3L, 4L, 5L),
        consumptions = listOf(1200.0, 1300.0, 1500.0, 1400.0, 1800.0),
        productions = listOf(400.0, 500.0, 300.0, 450.0, 350.0),
        lastUpdateTime = System.currentTimeMillis(),
        maxConsumption = 1800.0,
        averageConsumption = 1440.0,
        maxProduction = 500.0,
        averageProduction = 400.0,
    )
    ConsumptionWidgetContent(data, previewBitmap = generatePreviewBitmap(data))
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 180)
@Composable
fun PreviewConsumptionWidgetConsumptionOnly() {
    val data = WidgetConsumptionData(
        timestamps = listOf(1L, 2L, 3L, 4L, 5L),
        consumptions = listOf(800.0, 850.0, 900.0, 750.0, 1000.0),
        productions = emptyList(),
        lastUpdateTime = System.currentTimeMillis(),
        maxConsumption = 1000.0,
        averageConsumption = 860.0,
        maxProduction = 0.0,
        averageProduction = 0.0,
    )
    ConsumptionWidgetContent(data, previewBitmap = generatePreviewBitmap(data))
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 180)
@Composable
fun PreviewConsumptionWidgetEmpty() {
    ConsumptionWidgetContent(WidgetConsumptionData.empty())
}
