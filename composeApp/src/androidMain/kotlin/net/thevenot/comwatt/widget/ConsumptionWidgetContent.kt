package net.thevenot.comwatt.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import co.touchlab.kermit.Logger
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import net.thevenot.comwatt.MainActivity
import net.thevenot.comwatt.R
import net.thevenot.comwatt.ui.theme.AppTheme
import java.io.File
import kotlin.time.Instant

internal val WIDGET_DATA_KEY = stringPreferencesKey("widget_data_json")

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Composable
fun ConsumptionWidgetContent() {
    val context = LocalContext.current
    val prefs = currentState<Preferences>()
    val widgetData = parseWidgetData(prefs)
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(openAppIntent))
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                WidgetHeader()
                Spacer(modifier = GlanceModifier.height(AppTheme.dimens.paddingSmall))

                if (widgetData.hasData()) {
                    WidgetDataContent(context, widgetData)
                } else {
                    WidgetEmptyState()
                }
            }

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                CircleIconButton(
                    imageProvider = ImageProvider(R.drawable.ic_refresh_dark),
                    contentDescription = "Refresh",
                    onClick = actionRunCallback<RefreshWidgetAction>()
                )
            }
        }
    }
}

private fun parseWidgetData(prefs: Preferences): WidgetConsumptionData {
    return prefs[WIDGET_DATA_KEY]?.let { jsonString ->
        try {
            json.decodeFromString<WidgetConsumptionData>(jsonString)
        } catch (e: Exception) {
            Logger.withTag("ConsumptionWidgetContent").e(e) { "Failed to parse widget data" }
            WidgetConsumptionData.empty()
        }
    } ?: WidgetConsumptionData.empty()
}

private fun WidgetConsumptionData.hasData(): Boolean =
    consumptions.isNotEmpty() || productions.isNotEmpty()

@Composable
private fun WidgetHeader() {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_bolt),
            contentDescription = "Energy",
            modifier = GlanceModifier.size(18.dp)
        )
        Spacer(modifier = GlanceModifier.width(AppTheme.dimens.paddingExtraSmall))
        Text(
            text = "Energy Overview",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurface
            )
        )
    }
}

@Composable
private fun ColumnScope.WidgetDataContent(context: Context, widgetData: WidgetConsumptionData) {
    PowerStatsRow(widgetData)
    Spacer(modifier = GlanceModifier.height(6.dp))
    ChartImage(context, widgetData, GlanceModifier.fillMaxWidth().defaultWeight())
    Spacer(modifier = GlanceModifier.height(4.dp))
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(end = AppTheme.dimens.paddingSmall),
        horizontalAlignment = Alignment.End,
    ) {
        LastUpdateText(widgetData.lastUpdateTime)
    }
}

@Composable
private fun PowerStatsRow(widgetData: WidgetConsumptionData) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (widgetData.consumptions.isNotEmpty()) {
            PowerStat(
                iconRes = R.drawable.ic_arrow_down_consumption,
                value = widgetData.consumptions.lastOrNull()?.toInt() ?: 0,
                contentDescription = "Consumption"
            )
        }

        if (widgetData.consumptions.isNotEmpty() && widgetData.productions.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.width(12.dp))
        }

        if (widgetData.productions.isNotEmpty()) {
            PowerStat(
                iconRes = R.drawable.ic_arrow_up_production,
                value = widgetData.productions.lastOrNull()?.toInt() ?: 0,
                contentDescription = "Production"
            )
        }
    }
}

@Composable
private fun PowerStat(iconRes: Int, value: Int, contentDescription: String) {
    Image(
        provider = ImageProvider(iconRes),
        contentDescription = contentDescription,
        modifier = GlanceModifier.width(14.dp).height(14.dp)
    )
    Spacer(modifier = GlanceModifier.width(2.dp))
    Text(
        text = "${value}W",
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GlanceTheme.colors.onSurface
        )
    )
}

@Composable
private fun ChartImage(
    context: Context,
    widgetData: WidgetConsumptionData,
    modifier: GlanceModifier
) {
    val chartImageFile = File(context.filesDir, "widget_chart.png")
    val bitmap = if (chartImageFile.exists()) {
        BitmapFactory.decodeFile(chartImageFile.absolutePath)
    } else null

    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Energy chart",
            modifier = modifier
        )
    } else {
        ChartFallback(widgetData)
    }
}

@Composable
private fun ChartFallback(data: WidgetConsumptionData) {
    Text(
        text = createAsciiChart(data),
        style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.primary)
    )
}

private fun createAsciiChart(data: WidgetConsumptionData): String {
    if (data.consumptions.isEmpty() || data.maxConsumption == 0.0) return ""

    val blocks = listOf("▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")
    val maxIndex = blocks.size - 1

    return data.consumptions.takeLast(40).joinToString("") { value ->
        val normalized = (value / data.maxConsumption * maxIndex).toInt().coerceIn(0, maxIndex)
        blocks[normalized]
    }
}

@Composable
private fun LastUpdateText(lastUpdateTime: Long) {
    val text = if (lastUpdateTime > 0) {
        val instant = Instant.fromEpochMilliseconds(lastUpdateTime)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "Last update: %02d:%02d".format(localDateTime.hour, localDateTime.minute)
    } else {
        "No data"
    }

    Text(
        text = text,
        style = TextStyle(fontSize = 9.sp, color = GlanceTheme.colors.onSurfaceVariant)
    )
}

@Composable
private fun WidgetEmptyState() {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No data available",
            style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Widget will update automatically",
            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
        )
    }
}

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