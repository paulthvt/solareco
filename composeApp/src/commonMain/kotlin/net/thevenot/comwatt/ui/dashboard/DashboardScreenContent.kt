package net.thevenot.comwatt.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import co.touchlab.kermit.Logger
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
import comwatt.composeapp.generated.resources.day_range_selected_time_n_days_gao
import comwatt.composeapp.generated.resources.day_range_selected_time_today
import comwatt.composeapp.generated.resources.day_range_selected_time_yesterday
import comwatt.composeapp.generated.resources.hour_range_selected_time
import comwatt.composeapp.generated.resources.range_picker_button_custom
import comwatt.composeapp.generated.resources.range_picker_button_day
import comwatt.composeapp.generated.resources.range_picker_button_hour
import comwatt.composeapp.generated.resources.range_picker_button_week
import comwatt.composeapp.generated.resources.week_range_selected_time_n_weeks_ago
import comwatt.composeapp.generated.resources.week_range_selected_time_one_week_ago
import comwatt.composeapp.generated.resources.week_range_selected_time_past_seven_days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchTimeSeriesUseCase
import net.thevenot.comwatt.domain.model.ChartTimeSeries
import net.thevenot.comwatt.domain.model.TimeSeries
import net.thevenot.comwatt.domain.model.TimeSeriesTitle
import net.thevenot.comwatt.domain.model.TimeSeriesType
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerWithdrawals
import net.thevenot.comwatt.utils.formatDayMonth
import net.thevenot.comwatt.utils.formatTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.pow

private val LegendLabelKey = ExtraStore.Key<Set<String>>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenContent(
    dataRepository: DataRepository, viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(FetchTimeSeriesUseCase(dataRepository), dataRepository)
    }
) {
    LifecycleResumeEffect(Unit) {
        viewModel.startAutoRefresh()
        onPauseOrDispose {
            viewModel.stopAutoRefresh()
        }
    }
    val showDatePickerDialog = remember { mutableStateOf(false) }
    val charts by viewModel.charts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val state = rememberPullToRefreshState()

    if (showDatePickerDialog.value) {
        TimePickerDialog(
            selectedTimeUnitIndex = uiState.timeUnitSelectedIndex,
            onDismiss = { showDatePickerDialog.value = false },
            defaultSelectedTimeRange = uiState.selectedTimeRange,
            onRangeSelected = { range ->
                viewModel.onTimeSelected(range)
                showDatePickerDialog.value = false
            }
        )
    }

    LoadingView(uiState.isDataLoaded.not()) {
        PullToRefreshBox(state = state, isRefreshing = uiState.isRefreshing, onRefresh = {
            viewModel.singleRefresh()
        }) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = AppTheme.dimens.paddingNormal),
                verticalArrangement = Arrangement.spacedBy(
                    AppTheme.dimens.paddingNormal, Alignment.Top
                ),
                contentPadding = PaddingValues(bottom = AppTheme.dimens.paddingNormal)
            ) {
                item {
                    TimeUnitBar(uiState) { viewModel.onTimeUnitSelected(it) }
                }

                item {
                    RangeButton(
                        uiState = uiState,
                        onPreviousButtonClick = {
                            viewModel.dragRange(RangeSelectionButton.PREV)
                            viewModel.singleRefresh()
                        },
                        onNextButtonClick = {
                            viewModel.dragRange(RangeSelectionButton.NEXT)
                            viewModel.singleRefresh()
                        }) {
                        showDatePickerDialog.value = true
                    }
                }
                if (charts.isNotEmpty()) {
                    items(
                        items = charts.withIndex()
                            .filter { it.value.timeSeries.any { series -> series.values.isNotEmpty() } },
                        key = { it.index to it.value.name }) { (_, chart) ->
                        LazyGraphCard(uiState, chart)
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeButton(
    uiState: DashboardScreenState,
    onPreviousButtonClick: () -> Unit = {},
    onNextButtonClick: () -> Unit = {},
    showDatePickerDialog: () -> Unit
) {
    val selectedValue = when (uiState.timeUnitSelectedIndex) {
        0 -> uiState.selectedTimeRange.hour.selectedValue
        1 -> uiState.selectedTimeRange.day.selectedValue
        2 -> uiState.selectedTimeRange.week.selectedValue
        else -> 0
    }
    val minBound = when (uiState.timeUnitSelectedIndex) {
        0 -> 23
        1 -> 364
        2 -> 52
        else -> 0
    }
    Logger.d(TAG) { "selectedIndex $selectedValue" }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.paddingNormal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.timeUnitSelectedIndex != 3) {
            OutlinedIconButton(onClick = {
                onPreviousButtonClick()
            }, enabled = selectedValue < minBound) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
            }
        }

        TextButton(
            onClick = { showDatePickerDialog() },
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(AppTheme.dimens.paddingNormal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Logger.d(TAG) { "selected value ${uiState.selectedTimeRange.day.selectedValue}" }
                Text(
                    when (uiState.timeUnitSelectedIndex) {
                        0 -> pluralStringResource(
                            Res.plurals.hour_range_selected_time,
                            uiState.selectedTimeRange.hour.selectedValue + 1,
                            uiState.selectedTimeRange.hour.selectedValue + 1
                        )

                        1 -> when (uiState.selectedTimeRange.day.selectedValue) {
                            0 -> stringResource(Res.string.day_range_selected_time_today)
                            1 -> stringResource(Res.string.day_range_selected_time_yesterday)
                            else -> stringResource(
                                Res.string.day_range_selected_time_n_days_gao,
                                uiState.selectedTimeRange.day.selectedValue
                            )
                        }

                        2 -> when (uiState.selectedTimeRange.week.selectedValue) {
                            0 -> stringResource(Res.string.week_range_selected_time_past_seven_days)
                            1 -> stringResource(Res.string.week_range_selected_time_one_week_ago)
                            else -> stringResource(
                                Res.string.week_range_selected_time_n_weeks_ago,
                                uiState.selectedTimeRange.week.selectedValue
                            )
                        }

                        3 -> "${uiState.selectedTimeRange.custom.start.formatDayMonth()} - ${
                            uiState.selectedTimeRange.custom.end.formatDayMonth()
                        }"

                        else -> ""
                    }
                )

                when (uiState.timeUnitSelectedIndex) {
                    0 -> {
                        Text(
                            text = "${formatTime(uiState.selectedTimeRange.hour.start)} - ${
                                formatTime(
                                    uiState.selectedTimeRange.hour.end
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    2 -> {
                        Text(
                            text = "${uiState.selectedTimeRange.week.start.formatDayMonth()} - ${

                                uiState.selectedTimeRange.week.end.formatDayMonth()

                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    3 -> {
                        Text(
                            text = "${formatTime(uiState.selectedTimeRange.custom.start)} - ${
                                formatTime(
                                    uiState.selectedTimeRange.custom.end
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (uiState.timeUnitSelectedIndex != 3) {
            OutlinedIconButton(onClick = {
                onNextButtonClick()
            }, enabled = selectedValue > 0) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
            }
        }
    }
}

@Composable
private fun TimeUnitBar(
    uiState: DashboardScreenState,
    onTimeUnitSelected: (Int) -> Unit = {}
) {
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
                        onTimeUnitSelected(index)
                    }, selected = index == uiState.timeUnitSelectedIndex
                ) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun LazyGraphCard(uiState: DashboardScreenState, chart: ChartTimeSeries) {
    OutlinedCard {
        Column {
            Card {
                Chart(
                    timeSeries = chart.timeSeries, uiState = uiState
                )
            }

            Column(modifier = Modifier.padding(AppTheme.dimens.paddingNormal)) {
                ChartTitle(
                    chart.timeSeries.first().title.icon, chart.name?.trim() ?: "Unknown"
                )
            }
        }
    }
}

@Composable
fun Chart(
    timeSeries: List<TimeSeries>, modifier: Modifier = Modifier, uiState: DashboardScreenState
) {
    val chartsData = timeSeries.filter { it.values.values.isNotEmpty() }.map { it.values }
    val maxValue = remember(chartsData) {
        chartsData.flatMap { it.values }.maxOrNull() ?: 0f
    }
    val modelProducer = remember { CartesianChartModelProducer() }
    val markerValueFormatter = DefaultCartesianMarker.ValueFormatter.default(
        thousandsSeparator = " ", suffix = " W", decimalCount = 0
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
    val lineColors = getLineColors(timeSeries)
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
        if (stepValue <= 0.0) VerticalAxis.ItemPlacer.step() else VerticalAxis.ItemPlacer.step({
            stepValue
        })
    val rangeProvider =
        if (maxValue == 0f) CartesianLayerRangeProvider.auto() else CartesianLayerRangeProvider.fixed(
            maxY = maxValue.toDouble()
        )
    val legendItemLabelComponent =
        rememberTextComponent(MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground))

    val legend = createChartLegend(timeSeries, lineColors, legendItemLabelComponent)
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
                    }),
                rangeProvider = rangeProvider,
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = yAxisValueFormatter, label = rememberAxisLabelComponent(
                    minWidth = TextComponent.MinWidth.fixed(30.dp),
                ), itemPlacer = startAxisItemPlacer
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = rememberTimeValueFormatter(uiState.timeUnitSelectedIndex),
                itemPlacer = remember { TimeAlignedItemPlacer() }),
            marker = rememberMarker(markerValueFormatter),
            legend = legend,
        ),
        modelProducer = modelProducer,
        modifier = modifier.height(280.dp).padding(vertical = AppTheme.dimens.paddingSmall),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
    )
}

@Composable
private fun createChartLegend(
    timeSeries: List<TimeSeries>,
    lineColors: List<Color>,
    legendItemLabelComponent: TextComponent
) =
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
            start = AppTheme.dimens.paddingNormal, top = AppTheme.dimens.paddingSmall
        ),
    ) else null

@Composable
private fun getLineColors(timeSeries: List<TimeSeries>) = timeSeries.map {
    when (it.type) {
        TimeSeriesType.PRODUCTION -> MaterialTheme.colorScheme.powerProduction
        TimeSeriesType.CONSUMPTION -> MaterialTheme.colorScheme.powerConsumption
        TimeSeriesType.INJECTION -> MaterialTheme.colorScheme.powerInjection
        TimeSeriesType.WITHDRAWAL -> MaterialTheme.colorScheme.powerWithdrawals
    }
}

@Composable
fun rememberTimeValueFormatter(timeUnitSelectedIndex: Int): CartesianValueFormatter {
    return remember(timeUnitSelectedIndex) {
        CartesianValueFormatter { _, value, _ ->
            val instant = Instant.fromEpochSeconds(value.toLong())
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

            val hourMinutesFormat = LocalDateTime.Format { hour(); char(':'); minute() }
            val dayOfMonthFormat = LocalDateTime.Format {
                dayOfMonth()
                char(' ')
                monthName(MonthNames.ENGLISH_ABBREVIATED)
            }
            dateTime.format(
                when (timeUnitSelectedIndex) {
                    0, 1 -> if (dateTime.hour == 0 && dateTime.minute == 0) {
                        dayOfMonthFormat
                    } else {
                        hourMinutesFormat
                    }

                    2, 3 -> dayOfMonthFormat
                    else -> hourMinutesFormat
                }
            )
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

@Preview
@Composable
fun TimeUnitBarPreview() {
    val sampleState = DashboardScreenState(
        isDataLoaded = true,
        isRefreshing = false,
        selectedTimeRange = SelectedTimeRange(),
        timeUnitSelectedIndex = 1
    )

    ComwattTheme {
        TimeUnitBar(uiState = sampleState)
    }
}

@Preview
@Composable
fun RangeButtonPreview() {
    val sampleState = DashboardScreenState(
        isDataLoaded = true,
        isRefreshing = false,
        selectedTimeRange = SelectedTimeRange(
            hour = HourRange(
                selectedValue = 2,
                start = Instant.fromEpochSeconds(1_700_000_000L),
                end = Instant.fromEpochSeconds(1_700_003_600L)
            )
        ),
        timeUnitSelectedIndex = 0
    )

    ComwattTheme {
        RangeButton(
            uiState = sampleState,
            onPreviousButtonClick = {},
            onNextButtonClick = {},
            showDatePickerDialog = {}
        )
    }
}

@Preview
@Composable
fun LazyGraphCardPreview() {
    val sampleState = DashboardScreenState(
        isDataLoaded = true,
        isRefreshing = false,
        selectedTimeRange = SelectedTimeRange(),
        timeUnitSelectedIndex = 1
    )

    // Generate a list of sample data points (e.g., 6 hours)
    val now = Instant.fromEpochSeconds(1_700_000_000L)
    val sampleValues = (0..5).associate { i ->
        now.plus(i * 3600, kotlinx.datetime.DateTimeUnit.SECOND) to (100f + i * 50)
    }

    val sampleTimeSeries = TimeSeries(
        title = TimeSeriesTitle("Sample", Icons.Default.Info),
        type = TimeSeriesType.PRODUCTION,
        values = sampleValues
    )

    val sampleChart = ChartTimeSeries(
        name = "Sample Chart",
        timeSeries = listOf(sampleTimeSeries)
    )

    ComwattTheme {
        LazyGraphCard(uiState = sampleState, chart = sampleChart)
    }
}

private const val TAG = "DashboardScreenContent"