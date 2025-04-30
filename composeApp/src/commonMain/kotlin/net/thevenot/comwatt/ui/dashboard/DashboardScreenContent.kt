package net.thevenot.comwatt.ui.dashboard

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.multiplatform.cartesian.data.lineSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.multiplatform.common.Insets
import com.patrykandpatrick.vico.multiplatform.common.LegendItem
import com.patrykandpatrick.vico.multiplatform.common.component.ShapeComponent
import com.patrykandpatrick.vico.multiplatform.common.component.TextComponent
import com.patrykandpatrick.vico.multiplatform.common.component.rememberTextComponent
import com.patrykandpatrick.vico.multiplatform.common.data.ExtraStore
import com.patrykandpatrick.vico.multiplatform.common.fill
import com.patrykandpatrick.vico.multiplatform.common.rememberVerticalLegend
import com.patrykandpatrick.vico.multiplatform.common.shape.CorneredShape
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.range_picker_button_custom
import comwatt.composeapp.generated.resources.range_picker_button_day
import comwatt.composeapp.generated.resources.range_picker_button_hour
import comwatt.composeapp.generated.resources.range_picker_button_week
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import net.thevenot.comwatt.domain.model.TimeSeries
import net.thevenot.comwatt.domain.model.TimeSeriesType
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerWithdrawals
import org.jetbrains.compose.resources.stringResource
import kotlin.math.pow

private val LegendLabelKey = ExtraStore.Key<Set<String>>()

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
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .padding(AppTheme.dimens.paddingNormal),
                verticalArrangement = Arrangement.spacedBy(
                    AppTheme.dimens.paddingNormal,
                    Alignment.Top
                )
            ) {
                if (charts.isNotEmpty()) {
                    item {
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
                                        ), onClick = {
                                            viewModel.onTimeUnitSelected(index)
                                        }, selected = index == uiState.timeUnitSelectedIndex
                                    ) {
                                        Text(label)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        var dragDirection: HorizontalDragDirection? = null
                        Row(
                            modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
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
                            }, horizontalArrangement = Arrangement.Center
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
                    }
                    items(
                        items = charts.withIndex()
                            .filter { it.value.timeSeries.any { series -> series.values.isNotEmpty() } },
                        key = { it.index to it.value.name }) { (_, chart) ->
                        LazyGraphCard(chart, uiState)
                    }
                }
            }
        }
    }
}


@Composable
private fun LazyGraphCard(chart: ChartTimeSeries, uiState: DashboardScreenState) {
    OutlinedCard {
        Column {
            Card {
                Chart(
                    timeSeries = chart.timeSeries,
                    uiState = uiState
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

@Composable
fun Chart(
    timeSeries: List<TimeSeries>,
    modifier: Modifier = Modifier,
    uiState: DashboardScreenState
) {
    val chartsData = timeSeries.filter { it.values.values.isNotEmpty() }.map { it.values }
    val maxValue = remember(chartsData) {
        chartsData.flatMap { it.values }.maxOrNull() ?: 0f
    }
    val modelProducer = remember { CartesianChartModelProducer() }
    val markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
        thousandsSeparator = " ",
        suffix = " W",
        decimalCount = 0
    )

    LaunchedEffect(chartsData) {
        withContext(Dispatchers.Default) {
            modelProducer.runTransaction {
                lineSeries {
                    chartsData.forEach { data ->
                        series(
                            x = data.keys.map { it.epochSeconds }, y = data.values.toList()
                        )
                    }
                }
                extras { store ->
                    store[TimeAlignedItemPlacer.TimeUnitIndexKey] = uiState.timeUnitSelectedIndex
                    store[LegendLabelKey] = timeSeries.map { it.title.name }.toSet()
                }
            }
        }
    }
    val lineColors = timeSeries.map {
            when (it.type) {
                TimeSeriesType.PRODUCTION -> MaterialTheme.colorScheme.powerProduction
                TimeSeriesType.CONSUMPTION -> MaterialTheme.colorScheme.powerConsumption
                TimeSeriesType.INJECTION -> MaterialTheme.colorScheme.powerInjection
                TimeSeriesType.WITHDRAWAL -> MaterialTheme.colorScheme.powerWithdrawals
            }
        }
    Column {
        CartesianValueFormatter.decimal()
        val yAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
            when {
                value >= 1000 -> "${(value / 1000).toInt()}k"
                value < 1 && value > 0 -> value.toString()
                else -> value.toInt().toString()
            }
        }
        val stepValue = if (maxValue <= 0) 1.0 else {
            val magnitude = kotlin.math.floor(kotlin.math.log10(maxValue.toDouble()))
            10.0.pow(magnitude)
        }
        val startAxisItemPlacer =
            if (stepValue == 0.0) VerticalAxis.ItemPlacer.step() else VerticalAxis.ItemPlacer.step({
                stepValue
            })
        val rangeProvider =
            if (maxValue == 0f) CartesianLayerRangeProvider.auto() else CartesianLayerRangeProvider.fixed(
                maxY = maxValue.toDouble()
            )
        val legendItemLabelComponent =
            rememberTextComponent(MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground))

        val legend =
            if (timeSeries.size > 1) rememberVerticalLegend<CartesianMeasuringContext, CartesianDrawingContext>(
                items = { extraStore ->
                    extraStore[LegendLabelKey].forEachIndexed { index, label ->
                        add(
                            LegendItem(
                                ShapeComponent(fill(lineColors[index]), CorneredShape.Pill),
                                legendItemLabelComponent,
                                label,
                            )
                        )
                    }
                },
                padding = Insets(
                    start = AppTheme.dimens.paddingNormal,
                    top = AppTheme.dimens.paddingSmall
                ),
            ) else null
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        lineColors.map { color ->
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(fill(color)),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    fill(
                                        Brush.verticalGradient(
                                            listOf(
                                                color.copy(alpha = 0.4f), Color.Transparent
                                            )
                                        )
                                    )
                                ),
                            )
                        }
                    ),
                    rangeProvider = rangeProvider,
                ),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = yAxisValueFormatter,
                    label = rememberAxisLabelComponent(
                        minWidth = TextComponent.MinWidth.fixed(30.dp),
                    ),
                    itemPlacer = startAxisItemPlacer
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = rememberTimeValueFormatter(),
                    itemPlacer = remember { TimeAlignedItemPlacer() }
                ),
                marker = rememberMarker(markerValueFormatter),
                legend = legend,
            ),
            modelProducer = modelProducer,
            modifier = modifier.height(280.dp).padding(vertical = AppTheme.dimens.paddingSmall),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}

/**
 * Creates a time formatter that formats Instants the same way as your existing code
 */
@Composable
fun rememberTimeValueFormatter(): CartesianValueFormatter {
    return remember {
        CartesianValueFormatter { _, value, _ ->
            val instant = Instant.fromEpochSeconds(value.toLong())
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

            if (dateTime.hour == 0 && dateTime.minute == 0) {
                dateTime.format(
                    LocalDateTime.Format {
                        dayOfMonth(padding = Padding.SPACE)
                        monthName(MonthNames.ENGLISH_ABBREVIATED)
                    }
                )
            } else {
                dateTime.format(LocalDateTime.Format { hour(); char(':'); minute() })
            }
        }
    }
}

@Composable
fun ChartTitle(icon: ImageVector, title: String) {
    Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, "device_icon")
        Spacer(modifier = Modifier.width(AppTheme.dimens.paddingNormal))
        Text(
            title.uppercase(),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

private const val TAG = "DashboardScreenContent"