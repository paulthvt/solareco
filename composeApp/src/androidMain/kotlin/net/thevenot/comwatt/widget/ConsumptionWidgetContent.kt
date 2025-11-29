package net.thevenot.comwatt.widget

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import co.touchlab.kermit.Logger
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.di.dataStore
import java.io.File

@Composable
fun ConsumptionWidgetContent() {
    val context = LocalContext.current

    // Load widget data repository
    val widgetDataRepository = try {
        val factory = net.thevenot.comwatt.di.Factory(context)
        WidgetDataRepository(factory.dataStore)
    } catch (e: Exception) {
        Logger.withTag("ConsumptionWidgetContent")
            .e(e) { "Failed to create widget data repository" }
        null
    }

    // Get widget data
    val widgetData by (widgetDataRepository?.getWidgetData() ?: kotlinx.coroutines.flow.flowOf(
        WidgetConsumptionData.empty()
    )).collectAsState(
        initial = WidgetConsumptionData.empty()
    )

    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = "Consumption",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Statistics
            if (widgetData.consumptions.isNotEmpty()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    StatisticItem(
                        label = "Current",
                        value = "${widgetData.consumptions.lastOrNull()?.toInt() ?: 0} W",
                        valueColor = GlanceTheme.colors.primary
                    )

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    StatisticItem(
                        label = "Average",
                        value = "${widgetData.averageConsumption.toInt()} W",
                        valueColor = GlanceTheme.colors.onSurface
                    )

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    StatisticItem(
                        label = "Peak",
                        value = "${widgetData.maxConsumption.toInt()} W",
                        valueColor = GlanceTheme.colors.tertiary
                    )
                }

                Spacer(modifier = GlanceModifier.height(12.dp))

                // Try to load and display chart image
                val chartImageFile = File(context.filesDir, "widget_chart.png")
                if (chartImageFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(chartImageFile.absolutePath)
                    if (bitmap != null) {
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription = "Consumption chart",
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )
                    } else {
                        // Fallback to ASCII chart
                        ChartFallback(widgetData)
                    }
                } else {
                    // Fallback to ASCII chart
                    ChartFallback(widgetData)
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Last update time
                val lastUpdate = if (widgetData.lastUpdateTime > 0) {
                    val instant = Instant.fromEpochMilliseconds(widgetData.lastUpdateTime)
                    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                    "Updated: ${
                        String.format(
                            java.util.Locale.US,
                            "%02d:%02d",
                            localDateTime.hour,
                            localDateTime.minute
                        )
                    }"
                } else {
                    "No data"
                }

                Text(
                    text = lastUpdate,
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            } else {
                // No data state
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No data available",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Widget will update automatically",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    valueColor: androidx.glance.unit.ColorProvider
) {
    Column {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        )
    }
}

@Composable
private fun ChartFallback(data: WidgetConsumptionData) {
    Text(
        text = createSimpleChart(data),
        style = TextStyle(
            fontSize = 10.sp,
            color = GlanceTheme.colors.primary
        )
    )
}

/**
 * Create a simple ASCII-style chart representation (fallback)
 */
private fun createSimpleChart(data: WidgetConsumptionData): String {
    if (data.consumptions.isEmpty()) return ""

    val maxValue = data.maxConsumption
    if (maxValue == 0.0) return ""

    val height = 8 // Number of rows in chart
    val blocks = listOf("▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")

    return data.consumptions.takeLast(40).joinToString("") { value ->
        val normalized = (value / maxValue * (height - 1)).toInt().coerceIn(0, height - 1)
        blocks[normalized]
    }
}

