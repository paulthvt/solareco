package net.thevenot.comwatt.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import com.patrykandpatrick.vico.multiplatform.common.Fill
import com.patrykandpatrick.vico.multiplatform.common.Insets
import com.patrykandpatrick.vico.multiplatform.common.LegendItem
import com.patrykandpatrick.vico.multiplatform.common.component.ShapeComponent
import com.patrykandpatrick.vico.multiplatform.common.component.TextComponent
import com.patrykandpatrick.vico.multiplatform.common.component.rememberTextComponent
import com.patrykandpatrick.vico.multiplatform.common.data.ExtraStore
import com.patrykandpatrick.vico.multiplatform.common.rememberVerticalLegend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchTimeSeriesUseCase
import net.thevenot.comwatt.domain.model.TimeSeries
import net.thevenot.comwatt.domain.model.TimeSeriesType
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerWithdrawals
import net.thevenot.comwatt.utils.ScreenOrientationController
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val FullscreenLegendLabelKey = ExtraStore.Key<Set<String>>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenChartScreen(
    navController: NavController,
    dataRepository: DataRepository,
    chartIndex: Int,
    viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(FetchTimeSeriesUseCase(dataRepository), dataRepository)
    }
) {
    // Lock to landscape when entering, unlock when leaving
    DisposableEffect(Unit) {
        ScreenOrientationController.lockLandscape()
        onDispose {
            ScreenOrientationController.unlock()
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.startAutoRefresh()
        onPauseOrDispose {
            viewModel.stopAutoRefresh()
        }
    }

    val charts by viewModel.charts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val chart = charts.getOrNull(chartIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = chart?.name?.trim() ?: "Chart",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LoadingView(
            isLoading = !uiState.isDataLoaded,
            hasError = uiState.lastErrorMessage.isNotEmpty(),
            onRefresh = viewModel::singleRefresh
        ) {
            if (chart != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(AppTheme.dimens.paddingNormal),
                    contentAlignment = Alignment.Center
                ) {
                    FullscreenChart(
                        timeSeries = chart.timeSeries,
                        uiState = uiState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chart not available")
                }
            }
        }
    }
}

@Composable
private fun FullscreenChart(
    timeSeries: List<TimeSeries>,
    uiState: DashboardScreenState,
    modifier: Modifier = Modifier
) {
    val chartsData = remember(timeSeries) {
        timeSeries.filter { it.values.values.isNotEmpty() }.map { it.values }
    }
    val maxValue = remember(chartsData) { chartsData.flatMap { it.values }.maxOrNull() ?: 0f }
    val modelProducer = remember { CartesianChartModelProducer() }
    val markerValueFormatter = remember {
        DefaultCartesianMarker.ValueFormatter.default(
            thousandsSeparator = " ",
            suffix = " W",
            decimalCount = 0
        )
    }
    val rangeDuration = remember(chartsData) { calculateRangeDuration(chartsData) }

    val colorScheme = MaterialTheme.colorScheme
    val lineColors = remember(timeSeries, colorScheme) {
        timeSeries.map {
            when (it.type) {
                TimeSeriesType.PRODUCTION -> colorScheme.powerProduction
                TimeSeriesType.CONSUMPTION -> colorScheme.powerConsumption
                TimeSeriesType.INJECTION -> colorScheme.powerInjection
                TimeSeriesType.WITHDRAWAL -> colorScheme.powerWithdrawals
            }
        }
    }

    LaunchedEffect(timeSeries, uiState.selectedTimeUnit) {
        withContext(Dispatchers.Default) {
            modelProducer.runTransaction {
                lineSeries {
                    chartsData.forEach { data ->
                        series(x = data.keys.map { it.epochSeconds }, y = data.values.toList())
                    }
                }
                extras { store ->
                    store[TimeAlignedItemPlacer.TimeUnitIndexKey] = uiState.selectedTimeUnit
                    store[TimeAlignedItemPlacer.RangeDurationKey] = rangeDuration
                    store[FullscreenLegendLabelKey] = timeSeries.map { it.title.name }.toSet()
                }
            }
        }
    }

    val yAxisValueFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            when {
                value >= 1000 -> "${(value / 1000).toInt()}k"
                value < 1 && value > 0 -> value.toString()
                else -> value.toInt().toString()
            }
        }
    }
    val stepValue = if (maxValue <= 0) 1.0 else {
        val magnitude = kotlin.math.floor(kotlin.math.log10(maxValue.toDouble()))
        10.0.pow(magnitude)
    }
    val startAxisItemPlacer =
        if (stepValue <= 0.0) VerticalAxis.ItemPlacer.step() else VerticalAxis.ItemPlacer.step({
            stepValue
        })
    val rangeProvider = CartesianLayerRangeProvider.fixed(
        minY = 0.0, maxY = if (maxValue == 0f) 1.0 else maxValue.toDouble()
    )
    val legendItemLabelComponent =
        rememberTextComponent(MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground))

    val legend = createFullscreenChartLegend(timeSeries, lineColors, legendItemLabelComponent)

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    lineColors.map { color ->
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(Fill(color)),
                            areaFill = LineCartesianLayer.AreaFill.single(
                                Fill(
                                    Brush.verticalGradient(
                                        listOf(
                                            color.copy(alpha = 0.4f), Color.Transparent
                                        )
                                    )
                                )
                            ),
                            pointProvider = LineCartesianLayer.PointProvider.single(
                                LineCartesianLayer.Point(
                                    component = ShapeComponent(Fill(color), CircleShape),
                                    size = 4.dp
                                )
                            ),
                        )
                    }),
                rangeProvider = rangeProvider,
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = yAxisValueFormatter,
                label = rememberAxisLabelComponent(minWidth = TextComponent.MinWidth.fixed(30.dp)),
                itemPlacer = startAxisItemPlacer
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = rememberTimeValueFormatter(rangeDuration),
                itemPlacer = remember { TimeAlignedItemPlacer() }),
            marker = rememberMarker(markerValueFormatter),
            legend = legend,
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
    )
}

@Composable
private fun createFullscreenChartLegend(
    timeSeries: List<TimeSeries>,
    lineColors: List<Color>,
    legendItemLabelComponent: TextComponent
) =
    if (timeSeries.size > 1) rememberVerticalLegend<CartesianMeasuringContext, CartesianDrawingContext>(
        items = { extraStore ->
            val labels = extraStore[FullscreenLegendLabelKey]
            labels.forEachIndexed { index, label ->
                add(
                    LegendItem(
                        ShapeComponent(Fill(lineColors[index]), CircleShape),
                        legendItemLabelComponent,
                        label,
                    )
                )
            }
        },
        padding = Insets(start = AppTheme.dimens.paddingNormal, top = AppTheme.dimens.paddingSmall),
    ) else null

/**
 * Calculate the duration between the earliest and latest data points in the chart.
 */
private fun calculateRangeDuration(chartsData: List<Map<Instant, Float>>): Duration {
    if (chartsData.isEmpty() || chartsData.all { it.isEmpty() }) return 1.days

    val allTimestamps = chartsData.flatMap { it.keys }
    val earliestTimestamp = allTimestamps.minOrNull() ?: return 1.days
    val latestTimestamp = allTimestamps.maxOrNull() ?: return 1.days

    val durationSeconds = latestTimestamp.epochSeconds - earliestTimestamp.epochSeconds
    return durationSeconds.seconds
}
