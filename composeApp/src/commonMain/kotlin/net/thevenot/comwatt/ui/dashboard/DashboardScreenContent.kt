package net.thevenot.comwatt.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.range_picker_button_custom
import comwatt.composeapp.generated.resources.range_picker_button_day
import comwatt.composeapp.generated.resources.range_picker_button_hour
import comwatt.composeapp.generated.resources.range_picker_button_week
import io.github.aakira.napier.Napier
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.line.AreaBaseline
import io.github.koalaplot.core.line.AreaPlot
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.XYGraphScope
import io.github.koalaplot.core.xygraph.autoScaleYRange
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchTimeSeriesUseCase
import net.thevenot.comwatt.domain.model.ChartTimeSeries
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenContent(
    dataRepository: DataRepository,
    viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(FetchTimeSeriesUseCase(dataRepository), dataRepository)
    }
) {
    LifecycleResumeEffect(Unit) {
        viewModel.startAutoRefresh()
        onPauseOrDispose {
            viewModel.stopAutoRefresh()
        }
    }

    val charts by viewModel.charts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val state = rememberPullToRefreshState()

    LoadingView(uiState.isDataLoaded.not()) {
        PullToRefreshBox(state = state, isRefreshing = uiState.isRefreshing, onRefresh = {
            viewModel.singleRefresh()
        }) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppTheme.dimens.paddingNormal),
                verticalArrangement = Arrangement.spacedBy(
                    AppTheme.dimens.paddingNormal,
                    Alignment.Top
                )
            ) {
                if (charts.isNotEmpty()) {
                    Row {
                        val options = listOf(
                            stringResource(Res.string.range_picker_button_hour),
                            stringResource(Res.string.range_picker_button_day),
                            stringResource(Res.string.range_picker_button_week),
                            stringResource(Res.string.range_picker_button_custom)
                        )
                        SingleChoiceSegmentedButtonRow {
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index, count = options.size
                                    ),
                                    onClick = {
                                        viewModel.onTimeUnitSelected(index)
                                    },
                                    selected = index == uiState.timeUnitSelectedIndex
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }

                    var dragDirection: HorizontalDragDirection? = null
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(onDragEnd = {
                                    Napier.d(tag = TAG) { "Drag end $dragDirection" }
                                }) { _, dragAmount ->
                                    when {
                                        dragAmount < -20f -> {
                                            dragDirection = HorizontalDragDirection.LEFT
                                        }

                                        dragAmount > 20f -> {
                                            dragDirection = HorizontalDragDirection.RIGHT
                                        }
                                    }
                                }
                            },
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { Napier.d(tag = TAG) { "Click" } }) {
                            Text(
                                "Today",
                                modifier = Modifier.padding(AppTheme.dimens.paddingNormal)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    charts.forEach { chart ->
                        if (chart.timeSeries.any { it.values.values.isNotEmpty() }) {
                            GraphCard(chart)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphCard(chart: ChartTimeSeries) {
    val visible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible.value = true
    }

    AnimatedVisibility(
        visible = visible.value,
        enter = slideInVertically(initialOffsetY = { it })
    ) {
        OutlinedCard {
            Column {
                Card {
                    Chart(
                        chartName = chart.name,
                        chartsData = chart.timeSeries
                            .filter { it.values.values.isNotEmpty() }
                            .map { it.values }
                    )
                }
                Column(modifier = Modifier.padding(AppTheme.dimens.paddingNormal)) {
                    ChartTitle(
                        chart.timeSeries.first().title.icon,
                        chart.name?.trim() ?: "Unknown"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun Chart(chartName: String?, chartsData: List<Map<Instant, Float>>) {
    ChartLayout(
        modifier = Modifier.height(200.dp).fillMaxWidth().padding(AppTheme.dimens.paddingNormal)
    ) {
        val maxValue = chartsData.flatMap { it.values }.maxOrNull() ?: 0f
        Napier.d { "chartName: $chartName, maxValue: $maxValue, range values ${ 0f..(ceil(maxValue / 50.0) * 50.0).toFloat()}" }
        val combinedMaps = chartsData.map { chartData ->
            chartData.map { DefaultPoint(it.key, it.value) }
        }

        val yRanges = combinedMaps.map { it.autoScaleYRange() }
        val yRange = mergeRanges(yRanges)

        val minMajorTickIncrement = ((yRange.endInclusive - yRange.start) / 5).let {  ((it / 100).roundToInt() * 100).toFloat() }
        val allKeys = chartsData.flatMap { it.keys }
        val xRange = allKeys.min()..allKeys.max()

        XYGraph(
            xAxisModel = rememberTimeAxisModel(
                rangeProvider = {
                    xRange
                },
                minimumMajorTickSpacing = 50.dp
            ),
            yAxisModel = rememberFloatLinearAxisModel(
                range = yRange,
                minimumMajorTickSpacing = 50.dp,
                minorTickCount = 0,
                minimumMajorTickIncrement = minMajorTickIncrement
            ),
            xAxisLabels = {
                AxisLabel(
                    formatXAxisLabel(it), Modifier.padding(top = 2.dp)
                )
            },
            yAxisLabels = {
                AxisLabel(formatYAxisLabel(it), Modifier.absolutePadding(right = 2.dp))
            },
            yAxisTitle = {},
            horizontalMajorGridLineStyle = null,
            verticalMajorGridLineStyle = null,
            horizontalMinorGridLineStyle = null,
            verticalMinorGridLineStyle = null
        ) {
            combinedMaps.forEachIndexed { i, combinedMap ->
                chart(
                    data = combinedMap,
                    color = if(i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

fun mergeRanges(ranges: List<ClosedFloatingPointRange<Float>>): ClosedFloatingPointRange<Float> {
    val start = ranges.minOf { it.start }
    val end = ranges.maxOf { it.endInclusive }
    return start..end
}

fun formatXAxisLabel(timestamp: Instant): String {
    val dateTime = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
    Napier.d { "formatXAxisLabel: $dateTime" }
    return if (dateTime.hour == 0 && dateTime.minute == 0) {
        dateTime.format(LocalDateTime.Format {
            dayOfMonth(padding = Padding.NONE); char(' '); monthName(
            MonthNames.ENGLISH_ABBREVIATED
        )
        })
    } else {
        dateTime.format(LocalDateTime.Format { hour(); char(':'); minute() })
    }
}

fun formatYAxisLabel(value: Float): String {
    return when {
        value >= 1000 -> "${(value / 1000).toInt()}k"
        value < 1 && value > 0 -> value.toString()
        else -> value.toInt().toString()
    }
}

@Composable
private fun XYGraphScope<Instant, Float>.chart(
    data: List<DefaultPoint<Instant, Float>>,
    color: Color = MaterialTheme.colorScheme.secondary
) {
    AreaPlot(
        data = data,
        lineStyle = LineStyle(
            brush = SolidColor(color),
            strokeWidth = 2.dp
        ),
        areaStyle = AreaStyle(
            brush = SolidColor(color),
            alpha = 0.5f,
        ),
        areaBaseline = AreaBaseline.ConstantLine(0f)
    )
}

@Composable
fun HoverSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = Color.LightGray,
        modifier = modifier.padding(AppTheme.dimens.paddingNormal)
    ) {
        Box(modifier = Modifier.padding(AppTheme.dimens.paddingNormal)) {
            content()
        }
    }
}

@Composable
fun AxisTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
    )
}


@Composable
fun AxisLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}

@Composable
fun ChartTitle(icon: ImageVector, title: String) {
    Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, "device_icon")
        Spacer(modifier = Modifier.width(AppTheme.dimens.paddingNormal))
        Text(
            title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

private const val TAG = "DashboardScreenContent"