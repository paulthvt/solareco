package net.thevenot.comwatt.widget

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import kotlinx.coroutines.runBlocking

private const val DONUT_PREVIEW_SIZE = 200

private fun generatePreviewBitmaps(data: WidgetStatisticsData): StatisticsWidgetBitmaps {
    val generator = createStatisticsImageGenerator()
    return runBlocking {
        val selfConsBytes = generator.generateDonutImage(
            DonutImageParams(
                percentage = data.selfConsumptionRate?.toFloat(),
                label = "Self-cons.",
                sizePx = DONUT_PREVIEW_SIZE,
                isDarkMode = false,
                isSecondary = false,
            ),
        )
        val autonomyBytes = generator.generateDonutImage(
            DonutImageParams(
                percentage = data.autonomyRate.toFloat(),
                label = "Autonomy",
                sizePx = DONUT_PREVIEW_SIZE,
                isDarkMode = false,
                isSecondary = true,
            ),
        )
        StatisticsWidgetBitmaps(
            selfConsumptionDonut = selfConsBytes?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            },
            autonomyDonut = autonomyBytes?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            },
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 200)
@Composable
fun PreviewStatisticsWidgetWithData() {
    val data = WidgetStatisticsData(
        selfConsumptionRate = 0.72,
        autonomyRate = 0.85,
        totalProduction = 3200.0,
        totalConsumption = 2800.0,
        totalInjection = 900.0,
        totalWithdrawals = 420.0,
        lastUpdateTime = System.currentTimeMillis(),
    )
    StatisticsWidgetContent(data, previewBitmaps = generatePreviewBitmaps(data))
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 200)
@Composable
fun PreviewStatisticsWidgetHighSelfConsumption() {
    val data = WidgetStatisticsData(
        selfConsumptionRate = 0.95,
        autonomyRate = 0.60,
        totalProduction = 5000.0,
        totalConsumption = 4500.0,
        totalInjection = 250.0,
        totalWithdrawals = 1800.0,
        lastUpdateTime = System.currentTimeMillis(),
    )
    StatisticsWidgetContent(data, previewBitmaps = generatePreviewBitmaps(data))
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 200)
@Composable
fun PreviewStatisticsWidgetNoProduction() {
    val data = WidgetStatisticsData(
        selfConsumptionRate = null,
        autonomyRate = 0.0,
        totalProduction = 0.0,
        totalConsumption = 3000.0,
        totalInjection = 0.0,
        totalWithdrawals = 3000.0,
        lastUpdateTime = System.currentTimeMillis(),
    )
    StatisticsWidgetContent(data, previewBitmaps = generatePreviewBitmaps(data))
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 200)
@Composable
fun PreviewStatisticsWidgetEmpty() {
    StatisticsWidgetContent(WidgetStatisticsData.empty())
}
