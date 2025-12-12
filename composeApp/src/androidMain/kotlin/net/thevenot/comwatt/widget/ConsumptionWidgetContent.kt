package net.thevenot.comwatt.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.R
import net.thevenot.comwatt.di.dataStore
import java.io.File
import kotlin.time.Instant

// Power scheme colors (Dark mode values)
private val PowerProductionColor = Color(0xFF66BB6A)   // Green 400
private val PowerConsumptionColor = Color(0xFFFFB300)  // Amber 700

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
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Header with refresh button
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.width(6.dp))

                Text(
                    text = "Energy Overview",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // Refresh button
                CircleIconButton(
                    imageProvider = ImageProvider(R.drawable.ic_refresh_dark),
                    contentDescription = "Refresh",
                    onClick = actionRunCallback<RefreshWidgetAction>()
                )
            }

//            Spacer(modifier = GlanceModifier.height(6.dp))

            // Statistics - Compact layout
            if (widgetData.consumptions.isNotEmpty() || widgetData.productions.isNotEmpty()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Consumption
                    if (widgetData.consumptions.isNotEmpty()) {
                        Text(
                            text = "🔴",
                            style = TextStyle(fontSize = 10.sp)
                        )
                        Spacer(modifier = GlanceModifier.width(2.dp))
                        Text(
                            text = "${widgetData.consumptions.lastOrNull()?.toInt() ?: 0}W",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.error // Use error color for consumption (amber/red)
                            )
                        )
                    }

                    if (widgetData.consumptions.isNotEmpty() && widgetData.productions.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.width(12.dp))
                    }

                    // Production
                    if (widgetData.productions.isNotEmpty()) {
                        Text(
                            text = "🟢",
                            style = TextStyle(fontSize = 10.sp)
                        )
                        Spacer(modifier = GlanceModifier.width(2.dp))
                        Text(
                            text = "${widgetData.productions.lastOrNull()?.toInt() ?: 0}W",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.primary // Use primary color for production (green)
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Try to load and display chart image
                val chartImageFile = File(context.filesDir, "widget_chart.png")
                if (chartImageFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(chartImageFile.absolutePath)
                    if (bitmap != null) {
                        Image(
                            provider = ImageProvider(bitmap),
                            contentDescription = "Energy chart",
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .defaultWeight()
                        )
                    } else {
                        // Fallback to ASCII chart
                        ChartFallback(widgetData)
                    }
                } else {
                    // Fallback to ASCII chart
                    ChartFallback(widgetData)
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Last update time
                val lastUpdate = if (widgetData.lastUpdateTime > 0) {
                    val instant = Instant.fromEpochMilliseconds(widgetData.lastUpdateTime)
                    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                    val timeStr = String.format(
                        java.util.Locale.US,
                        "%02d:%02d",
                        localDateTime.hour,
                        localDateTime.minute
                    )
                    "Last update: $timeStr"
                } else {
                    "No data"
                }

                Text(
                    text = lastUpdate,
                    style = TextStyle(
                        fontSize = 9.sp,
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

/**
 * Action callback for widget refresh button
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Logger.withTag("RefreshWidgetAction").d { "Manual refresh triggered" }
        ConsumptionWidget.updateWidgetData(context)
    }
}