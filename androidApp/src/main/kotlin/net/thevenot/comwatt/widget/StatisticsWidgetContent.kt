package net.thevenot.comwatt.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import co.touchlab.kermit.Logger
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import net.thevenot.comwatt.MainActivity
import net.thevenot.comwatt.R
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.powerConsumptionLight
import net.thevenot.comwatt.ui.theme.powerInjectionLight
import net.thevenot.comwatt.ui.theme.powerProductionLight
import net.thevenot.comwatt.ui.theme.powerWithdrawalsLight
import kotlin.math.roundToInt
import kotlin.time.Instant

internal val STATISTICS_WIDGET_DATA_KEY = stringPreferencesKey("statistics_widget_data_json")

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

data class StatisticsWidgetBitmaps(
    val selfConsumptionDonut: android.graphics.Bitmap? = null,
    val autonomyDonut: android.graphics.Bitmap? = null,
)

@Composable
fun StatisticsWidgetContent() {
    val prefs = currentState<Preferences>()
    val widgetData = parseStatisticsData(prefs)
    StatisticsWidgetContent(widgetData)
}

@Composable
internal fun StatisticsWidgetContent(
    widgetData: WidgetStatisticsData,
    previewBitmaps: StatisticsWidgetBitmaps? = null,
) {
    val context = LocalContext.current
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(AppTheme.dimens.paddingNormal - AppTheme.dimens.paddingExtraSmall)
                .clickable(actionStartActivity(openAppIntent)),
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
            ) {
                WidgetHeader(context, widgetData.lastUpdateTime)
                Spacer(modifier = GlanceModifier.height(AppTheme.dimens.paddingSmall))

                if (widgetData.hasData()) {
                    DonutsRow(context, widgetData, previewBitmaps)
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    HorizontalBarsSection(widgetData)
                } else {
                    EmptyState(context)
                }
            }

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd,
            ) {
                CircleIconButton(
                    imageProvider = ImageProvider(R.drawable.ic_refresh_dark),
                    contentDescription = context.getString(R.string.widget_refresh_button),
                    onClick = actionRunCallback<RefreshStatisticsWidgetAction>(),
                )
            }
        }
    }
}

private fun parseStatisticsData(prefs: Preferences): WidgetStatisticsData {
    return prefs[STATISTICS_WIDGET_DATA_KEY]?.let { jsonString ->
        try {
            json.decodeFromString<WidgetStatisticsData>(jsonString)
        } catch (e: Exception) {
            Logger.withTag("StatisticsWidgetContent").e(e) { "Failed to parse statistics data" }
            WidgetStatisticsData.empty()
        }
    } ?: WidgetStatisticsData.empty()
}

@Composable
private fun WidgetHeader(context: Context, lastUpdateTime: Long) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_analytics),
            contentDescription = context.getString(R.string.widget_energy_icon),
            modifier = GlanceModifier.size(16.dp),
        )
        Spacer(modifier = GlanceModifier.width(AppTheme.dimens.paddingExtraSmall))
        Text(
            text = context.getString(R.string.statistics_widget_title),
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurface,
            ),
        )
        if (lastUpdateTime > 0) {
            val instant = Instant.fromEpochMilliseconds(lastUpdateTime)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val timeStr = "%02d:%02d".format(localDateTime.hour, localDateTime.minute)
            Text(
                text = " · ↻ $timeStr",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun DonutsRow(
    context: Context,
    data: WidgetStatisticsData,
    previewBitmaps: StatisticsWidgetBitmaps?,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DonutWithLabel(
            context = context,
            fileName = "widget_donut_self_consumption.png",
            previewBitmap = previewBitmaps?.selfConsumptionDonut,
            label = context.getString(R.string.statistics_self_consumption_label),
        )
        Spacer(modifier = GlanceModifier.width(24.dp))
        DonutWithLabel(
            context = context,
            fileName = "widget_donut_autonomy.png",
            previewBitmap = previewBitmaps?.autonomyDonut,
            label = context.getString(R.string.statistics_autonomy_label),
        )
    }
}

@Composable
private fun DonutWithLabel(
    context: Context,
    fileName: String,
    previewBitmap: android.graphics.Bitmap?,
    label: String,
) {
    val bitmap = previewBitmap ?: run {
        val file = java.io.File(context.filesDir, fileName)
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = label,
                modifier = GlanceModifier.size(64.dp),
            )
        } else {
            Spacer(modifier = GlanceModifier.size(64.dp))
        }
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = label,
            style = TextStyle(
                fontSize = 11.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun HorizontalBarsSection(data: WidgetStatisticsData) {
    val maxValue = maxOf(
        data.totalProduction,
        data.totalConsumption,
        data.totalInjection,
        data.totalWithdrawals,
    )
    if (maxValue <= 0.0) return

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        HorizontalBarRow(R.drawable.ic_stat_production, data.totalProduction, maxValue, BarColor.PRODUCTION)
        Spacer(modifier = GlanceModifier.height(1.dp))
        HorizontalBarRow(R.drawable.ic_stat_consumption, data.totalConsumption, maxValue, BarColor.CONSUMPTION)
        Spacer(modifier = GlanceModifier.height(1.dp))
        HorizontalBarRow(R.drawable.ic_stat_injection, data.totalInjection, maxValue, BarColor.INJECTION)
        Spacer(modifier = GlanceModifier.height(1.dp))
        HorizontalBarRow(R.drawable.ic_stat_withdrawals, data.totalWithdrawals, maxValue, BarColor.WITHDRAWALS)
    }
}

private enum class BarColor {
    PRODUCTION, CONSUMPTION, INJECTION, WITHDRAWALS
}

@Composable
private fun HorizontalBarRow(
    iconRes: Int,
    value: Double,
    maxValue: Double,
    barColor: BarColor,
) {
    val valueKwh = value / 1000.0
    val displayedKwh = if (valueKwh >= 10) {
        valueKwh.roundToInt().toDouble()
    } else {
        (valueKwh * 10).roundToInt() / 10.0
    }
    val maxKwh = maxValue / 1000.0
    val valueText = if (valueKwh >= 10) {
        "${valueKwh.roundToInt()} kWh"
    } else {
        "%.1f kWh".format(valueKwh)
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(16.dp),
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        BarIndicator(displayedKwh, maxKwh, barColor, GlanceModifier.defaultWeight().height(8.dp))
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = valueText,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurface,
            ),
            modifier = GlanceModifier.width(52.dp),
        )
    }
}

@Composable
private fun BarIndicator(
    value: Double,
    maxValue: Double,
    barColor: BarColor,
    modifier: GlanceModifier,
) {
    val fraction = if (maxValue > 0 && value > 0) {
        (value / maxValue).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val maxBarWidthDp = 150
    val barWidth = (maxBarWidthDp * fraction).toInt().coerceAtLeast(if (fraction > 0f) 4 else 0)
    val color = when (barColor) {
        BarColor.PRODUCTION -> ColorProvider(powerProductionLight)
        BarColor.CONSUMPTION -> ColorProvider(powerConsumptionLight)
        BarColor.INJECTION -> ColorProvider(powerInjectionLight)
        BarColor.WITHDRAWALS -> ColorProvider(powerWithdrawalsLight)
    }

    Box(modifier = modifier) {
        if (barWidth > 0) {
            Box(
                modifier = GlanceModifier
                    .width(barWidth.dp)
                    .fillMaxHeight()
                    .cornerRadius(4.dp)
                    .background(color),
            ) {}
        }
    }
}


@Composable
private fun EmptyState(context: Context) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(R.string.widget_no_data),
            style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
        Spacer(modifier = GlanceModifier.height(AppTheme.dimens.paddingExtraSmall))
        Text(
            text = context.getString(R.string.widget_will_update),
            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
    }
}
