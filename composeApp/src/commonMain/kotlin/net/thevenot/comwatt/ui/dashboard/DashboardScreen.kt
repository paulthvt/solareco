package net.thevenot.comwatt.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LineAxis
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
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
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.dashboard_chart_statistics_avg_title
import comwatt.composeapp.generated.resources.dashboard_chart_statistics_expand_icon_description_collapsed
import comwatt.composeapp.generated.resources.dashboard_chart_statistics_expand_icon_description_expanded
import comwatt.composeapp.generated.resources.dashboard_chart_statistics_max_title
import comwatt.composeapp.generated.resources.dashboard_chart_statistics_min_title
import comwatt.composeapp.generated.resources.dashboard_chart_statistics_sum_title
import comwatt.composeapp.generated.resources.dashboard_chart_statistics_title
import comwatt.composeapp.generated.resources.dashboard_screen_title
import comwatt.composeapp.generated.resources.day_range_selected_time_n_days_gao
import comwatt.composeapp.generated.resources.day_range_selected_time_today
import comwatt.composeapp.generated.resources.day_range_selected_time_yesterday
import comwatt.composeapp.generated.resources.error_fetching_data
import comwatt.composeapp.generated.resources.hour_range_selected_time
import comwatt.composeapp.generated.resources.range_picker_button_custom
import comwatt.composeapp.generated.resources.range_picker_button_day
import comwatt.composeapp.generated.resources.range_picker_button_hour
import comwatt.composeapp.generated.resources.range_picker_button_week
import comwatt.composeapp.generated.resources.statistics_card_title
import comwatt.composeapp.generated.resources.statistics_card_title_custom
import comwatt.composeapp.generated.resources.statistics_card_title_hourly
import comwatt.composeapp.generated.resources.statistics_card_title_weekly
import comwatt.composeapp.generated.resources.week_range_selected_time_n_weeks_ago
import comwatt.composeapp.generated.resources.week_range_selected_time_one_week_ago
import comwatt.composeapp.generated.resources.week_range_selected_time_past_seven_days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import net.thevenot.comwatt.ui.common.CenteredTitleWithIcon
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit
import net.thevenot.comwatt.ui.home.statistics.StatisticsCard
import net.thevenot.comwatt.ui.nav.NestedAppScaffold
import net.thevenot.comwatt.ui.preview.HotPreviewLightDark
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerWithdrawals
import net.thevenot.comwatt.utils.formatDayMonth
import net.thevenot.comwatt.utils.formatHourMinutes
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val LegendLabelKey = ExtraStore.Key<Set<String>>()
private const val TAG = "DashboardScreenContent"

@Composable
fun DashboardScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    dataRepository: DataRepository,
    viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(FetchTimeSeriesUseCase(dataRepository), dataRepository)
    }
) {
    DashboardScreenContent(navController, dataRepository, snackbarHostState, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenContent(
    navController: NavController,
    dataRepository: DataRepository,
    snackbarHostState: SnackbarHostState,
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
    val showDatePickerDialog = remember { mutableStateOf(false) }
    val charts by viewModel.charts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val state = rememberPullToRefreshState()
    val fetchErrorMessage = stringResource(Res.string.error_fetching_data)
    LaunchedEffect(uiState.lastErrorMessage) {
        if (uiState.lastErrorMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(fetchErrorMessage)
        }
    }

    if (showDatePickerDialog.value) {
        TimePickerDialog(
            selectedTimeUnit = uiState.selectedTimeUnit,
            onDismiss = { showDatePickerDialog.value = false },
            defaultSelectedTimeRange = uiState.selectedTimeRange,
            onRangeSelected = { range ->
                viewModel.onTimeSelected(range)
                showDatePickerDialog.value = false
            }
        )
    }

    NestedAppScaffold(
        navController = navController,
        title = {
            CenteredTitleWithIcon(
                icon = Icons.Filled.LineAxis,
                title = stringResource(Res.string.dashboard_screen_title),
                iconContentDescription = "Statistics Icon"
            )
        },
        snackbarHostState = snackbarHostState,
    ) {
        LoadingView(
            isLoading = uiState.isDataLoaded.not(),
            hasError = uiState.lastErrorMessage.isNotEmpty(),
            onRefresh = viewModel::singleRefresh
        ) {
            PullToRefreshBox(state = state, isRefreshing = uiState.isRefreshing, onRefresh = {
                viewModel.singleRefresh()
            }) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                        .padding(horizontal = AppTheme.dimens.paddingNormal),
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

                    uiState.rangeStats?.let { stats ->
                        item(key = "range_stats_card") {
                            val statsTitle = when (uiState.selectedTimeUnit) {
                                DashboardTimeUnit.HOUR -> stringResource(Res.string.statistics_card_title_hourly)
                                DashboardTimeUnit.DAY -> stringResource(Res.string.statistics_card_title)
                                DashboardTimeUnit.WEEK -> stringResource(Res.string.statistics_card_title_weekly)
                                DashboardTimeUnit.CUSTOM -> stringResource(Res.string.statistics_card_title_custom)
                            }
                            StatisticsCard(
                                siteDailyData = stats,
                                totalsLabel = buildRangeTotalsLabel(uiState),
                                modifier = Modifier.fillMaxWidth(),
                                title = statsTitle
                            )
                        }
                    }

                    if (charts.isNotEmpty()) {
                        items(
                            items = charts.withIndex()
                                .filter { it.value.timeSeries.any { series -> series.values.isNotEmpty() } },
                            key = { it.index to it.value.name }) { (_, chart) ->
                            LazyGraphCard(uiState, chart) { viewModel.toggleCardExpansion(it) }
                        }
                    }
                }
            }
        }
    }
}

private fun buildRangeTotalsLabel(uiState: DashboardScreenState): String =
    when (uiState.selectedTimeUnit) {
        DashboardTimeUnit.HOUR -> "${uiState.selectedTimeRange.hour.start.formatHourMinutes()} - ${uiState.selectedTimeRange.hour.end.formatHourMinutes()}"
        DashboardTimeUnit.DAY -> uiState.selectedTimeRange.day.end.formatDayMonth()
        DashboardTimeUnit.WEEK -> "${uiState.selectedTimeRange.week.start.formatDayMonth()} - ${uiState.selectedTimeRange.week.end.formatDayMonth()}"
        DashboardTimeUnit.CUSTOM -> "${uiState.selectedTimeRange.custom.start.formatDayMonth()} - ${uiState.selectedTimeRange.custom.end.formatDayMonth()}"
    }

@Composable
private fun RangeButton(
    uiState: DashboardScreenState,
    onPreviousButtonClick: () -> Unit = {},
    onNextButtonClick: () -> Unit = {},
    showDatePickerDialog: () -> Unit
) {
    val selectedValue = when (uiState.selectedTimeUnit) {
        DashboardTimeUnit.HOUR -> uiState.selectedTimeRange.hour.selectedValue
        DashboardTimeUnit.DAY -> uiState.selectedTimeRange.day.selectedValue
        DashboardTimeUnit.WEEK -> uiState.selectedTimeRange.week.selectedValue
        DashboardTimeUnit.CUSTOM -> 0
    }
    val minBound = when (uiState.selectedTimeUnit) {
        DashboardTimeUnit.HOUR -> 23
        DashboardTimeUnit.DAY -> 364
        DashboardTimeUnit.WEEK -> 52
        DashboardTimeUnit.CUSTOM -> 0
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.paddingNormal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.selectedTimeUnit != DashboardTimeUnit.CUSTOM) {
            OutlinedIconButton(
                onClick = onPreviousButtonClick,
                enabled = selectedValue < minBound
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
            }
        }

        TextButton(onClick = showDatePickerDialog, modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.padding(AppTheme.dimens.paddingNormal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    when (uiState.selectedTimeUnit) {
                        DashboardTimeUnit.HOUR -> pluralStringResource(
                            Res.plurals.hour_range_selected_time,
                            uiState.selectedTimeRange.hour.selectedValue + 1,
                            uiState.selectedTimeRange.hour.selectedValue + 1
                        )

                        DashboardTimeUnit.DAY -> when (uiState.selectedTimeRange.day.selectedValue) {
                            0 -> stringResource(Res.string.day_range_selected_time_today)
                            1 -> stringResource(Res.string.day_range_selected_time_yesterday)
                            else -> stringResource(
                                Res.string.day_range_selected_time_n_days_gao,
                                uiState.selectedTimeRange.day.selectedValue
                            )
                        }

                        DashboardTimeUnit.WEEK -> when (uiState.selectedTimeRange.week.selectedValue) {
                            0 -> stringResource(Res.string.week_range_selected_time_past_seven_days)
                            1 -> stringResource(Res.string.week_range_selected_time_one_week_ago)
                            else -> stringResource(
                                Res.string.week_range_selected_time_n_weeks_ago,
                                uiState.selectedTimeRange.week.selectedValue
                            )
                        }

                        DashboardTimeUnit.CUSTOM -> "${uiState.selectedTimeRange.custom.start.formatDayMonth()} - ${
                            uiState.selectedTimeRange.custom.end.formatDayMonth()
                        }"
                    }
                )

                when (uiState.selectedTimeUnit) {
                    DashboardTimeUnit.HOUR ->
                        Text(
                            text = "${uiState.selectedTimeRange.hour.start.formatHourMinutes()} - ${uiState.selectedTimeRange.hour.end.formatHourMinutes()}",
                            style = MaterialTheme.typography.bodySmall
                        )

                    DashboardTimeUnit.DAY ->
                        Text(
                            text = uiState.selectedTimeRange.day.end.formatDayMonth(),
                            style = MaterialTheme.typography.bodySmall
                        )

                    DashboardTimeUnit.WEEK ->
                        Text(
                            text = "${uiState.selectedTimeRange.week.start.formatDayMonth()} - ${
                                uiState.selectedTimeRange.week.end.formatDayMonth()
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )

                    DashboardTimeUnit.CUSTOM ->
                        Text(
                            text = "${uiState.selectedTimeRange.custom.start.formatHourMinutes()} - ${uiState.selectedTimeRange.custom.end.formatHourMinutes()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                }
            }
        }

        if (uiState.selectedTimeUnit != DashboardTimeUnit.CUSTOM) {
            OutlinedIconButton(onClick = onNextButtonClick, enabled = selectedValue > 0) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
            }
        }
    }
}

@Composable
private fun TimeUnitBar(
    uiState: DashboardScreenState,
    onTimeUnitSelected: (DashboardTimeUnit) -> Unit = {}
) {
    Row {
        val options = listOf(
            stringResource(Res.string.range_picker_button_hour) to DashboardTimeUnit.HOUR,
            stringResource(Res.string.range_picker_button_day) to DashboardTimeUnit.DAY,
            stringResource(Res.string.range_picker_button_week) to DashboardTimeUnit.WEEK,
            stringResource(Res.string.range_picker_button_custom) to DashboardTimeUnit.CUSTOM
        )
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, (label, timeUnit) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { onTimeUnitSelected(timeUnit) },
                    selected = timeUnit == uiState.selectedTimeUnit
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun LazyGraphCard(
    uiState: DashboardScreenState,
    chart: ChartTimeSeries,
    toggleCardExpansion: (String) -> Unit = {}
) {
    val isExpanded = uiState.expandedCards.contains(chart.name ?: "Unknown")

    OutlinedCard {
        Column {
            Card { Chart(timeSeries = chart.timeSeries, uiState = uiState) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = AppTheme.dimens.paddingNormal,
                    vertical = AppTheme.dimens.paddingSmall
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartTitle(
                    chart.timeSeries.first().title.icon,
                    chart.name?.trim() ?: "Unknown"
                )

                IconButton(
                    onClick = { toggleCardExpansion(chart.name ?: "Unknown") },
                    modifier = Modifier.width(32.dp).height(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded)
                            stringResource(Res.string.dashboard_chart_statistics_expand_icon_description_expanded)
                        else stringResource(Res.string.dashboard_chart_statistics_expand_icon_description_collapsed),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(AppTheme.dimens.paddingNormal)
                ) {
                    Text(
                        text = stringResource(Res.string.dashboard_chart_statistics_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = AppTheme.dimens.paddingSmall)
                    )

                    TimeSeriesStatisticsTable(
                        timeSeriesList = chart.timeSeries,
                        statisticsList = chart.statistics,
                        colors = chart.timeSeries.map { getLineColorForTimeSeries(it) },
                        modifier = Modifier.padding(bottom = AppTheme.dimens.paddingSmall)
                    )
                }
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
    val chartsData = remember(timeSeries) {
        timeSeries.filter { it.values.values.isNotEmpty() }.map { it.values }
    }
    val maxValue = remember(chartsData) { chartsData.flatMap { it.values }.maxOrNull() ?: 0f }
    val modelProducer = remember { CartesianChartModelProducer() }
    val markerValueFormatter = remember {
        DefaultCartesianMarker.ValueFormatter.default(
            thousandsSeparator = " ",
            suffix = " W",
            decimalCount = 0,
//            colorCode = timeSeries.size > 1
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
                    store[LegendLabelKey] = timeSeries.map { it.title.name }.toSet()
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
        minY = 0.0,
        maxY = if (maxValue == 0f) 1.0 else maxValue.toDouble()
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
                            fill = LineCartesianLayer.LineFill.single(Fill(color)),
                            areaFill = LineCartesianLayer.AreaFill.single(
                                Fill(
                                    Brush.verticalGradient(
                                        listOf(
                                            color.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                            ),
                            pointProvider = LineCartesianLayer.PointProvider.single(
                                LineCartesianLayer.Point(
                                    component = ShapeComponent(Fill(color), CircleShape),
                                    size = 0.dp
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

@Composable
private fun createChartLegend(
    timeSeries: List<TimeSeries>,
    lineColors: List<Color>,
    legendItemLabelComponent: TextComponent
) =
    if (timeSeries.size > 1) rememberVerticalLegend<CartesianMeasuringContext, CartesianDrawingContext>(
        items = { extraStore ->
            val labels = extraStore[LegendLabelKey]
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

@Composable
fun rememberTimeValueFormatter(rangeDuration: Duration): CartesianValueFormatter =
    remember(rangeDuration) {
        val hourMinutesFormat = LocalDateTime.Format { hour(); char(':'); minute() }
        val dayOfMonthFormat =
            LocalDateTime.Format { day(); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED) }
        val monthYearFormat = LocalDateTime.Format {
            day(); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); year()
        }
        CartesianValueFormatter { _, value, _ ->
            val instant = Instant.fromEpochSeconds(value.toLong())
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            dateTime.format(
                when {
                    rangeDuration < 4.hours -> hourMinutesFormat
                    rangeDuration < 1.days -> hourMinutesFormat
                    rangeDuration < 7.days -> dayOfMonthFormat
                    rangeDuration < 60.days -> dayOfMonthFormat
                    else -> monthYearFormat
                }
            )
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

/**
 * Calculate the duration between the earliest and latest data points in the chart.
 * This will be used to determine the appropriate interval for chart axis ticks.
 */
private fun calculateRangeDuration(chartsData: List<Map<Instant, Float>>): Duration {
    if (chartsData.isEmpty() || chartsData.all { it.isEmpty() }) return 1.days

    val allTimestamps = chartsData.flatMap { it.keys }
    val earliestTimestamp = allTimestamps.minOrNull() ?: return 1.days
    val latestTimestamp = allTimestamps.maxOrNull() ?: return 1.days

    val durationSeconds = latestTimestamp.epochSeconds - earliestTimestamp.epochSeconds
    return durationSeconds.seconds
}

private fun formatPowerValue(value: Double): String = when {
    value >= 1000 -> "${(value / 1000).toInt()} kW"
    value < 1 && value > 0 -> "${value.toInt()} W"
    else -> "${value.toInt()} W"
}

private fun formatEnergyValue(value: Double): String = when {
    value >= 1000 -> "${(value / 1000).toInt()} kWh"
    value < 1 && value > 0 -> "${value.toInt()} Wh"
    else -> "${value.toInt()} Wh"
}

@Composable
private fun getLineColorForTimeSeries(timeSeries: TimeSeries): Color = when (timeSeries.type) {
    TimeSeriesType.PRODUCTION -> MaterialTheme.colorScheme.powerProduction
    TimeSeriesType.CONSUMPTION -> MaterialTheme.colorScheme.powerConsumption
    TimeSeriesType.INJECTION -> MaterialTheme.colorScheme.powerInjection
    TimeSeriesType.WITHDRAWAL -> MaterialTheme.colorScheme.powerWithdrawals
}

@Composable
private fun TimeSeriesStatisticsTable(
    timeSeriesList: List<TimeSeries>,
    statisticsList: List<ChartStatistics>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = AppTheme.dimens.paddingSmall),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(Res.string.dashboard_chart_statistics_min_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.dashboard_chart_statistics_max_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.dashboard_chart_statistics_avg_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.dashboard_chart_statistics_sum_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        timeSeriesList.forEachIndexed { index, _ ->
            TimeSeriesStatisticsRow(
                statistics = statisticsList[index],
                color = colors[index],
                modifier = Modifier.padding(bottom = AppTheme.dimens.paddingExtraSmall)
            )
        }
    }
}

@Composable
private fun TimeSeriesStatisticsRow(
    statistics: ChartStatistics,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Canvas(modifier = Modifier.size(12.dp)) { drawCircle(color = color) }

        Text(
            text = formatPowerValue(statistics.min),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = formatPowerValue(statistics.max),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = formatPowerValue(statistics.average),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = formatEnergyValue(statistics.sum),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@HotPreviewLightDark
@Composable
fun TimeUnitBarPreview() {
    val sampleState = DashboardScreenState(
        isDataLoaded = true,
        isRefreshing = false,
        selectedTimeRange = SelectedTimeRange(),
        selectedTimeUnit = DashboardTimeUnit.HOUR
    )

    ComwattTheme {
        TimeUnitBar(uiState = sampleState)
    }
}

@HotPreviewLightDark
@Composable
fun RangeButtonPreview() {
    val sampleState = DashboardScreenState(
        isDataLoaded = true,
        isRefreshing = false,
        selectedTimeRange = SelectedTimeRange(
            hour = HourRange(
                selectedValue = 2,
                start = LocalDateTime(2025, 10, 7, 20, 41, 0, 0),
                end = LocalDateTime(2025, 10, 7, 21, 41, 0, 0)
            )
        ),
        selectedTimeUnit = DashboardTimeUnit.HOUR
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

@HotPreviewLightDark
@Composable
fun LazyGraphCardPreview() {
    val sampleState = DashboardScreenState(
        isDataLoaded = true,
        isRefreshing = false,
        selectedTimeRange = SelectedTimeRange(),
        selectedTimeUnit = DashboardTimeUnit.HOUR
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

@HotPreviewLightDark
@Composable
fun TimeSeriesStatisticsTablePreview() {
    val sampleTimeSeries1 = TimeSeries(
        title = TimeSeriesTitle("Production", Icons.Default.Info),
        type = TimeSeriesType.PRODUCTION,
        values = emptyMap()
    )

    val sampleTimeSeries2 = TimeSeries(
        title = TimeSeriesTitle("Consumption", Icons.Default.Info),
        type = TimeSeriesType.CONSUMPTION,
        values = emptyMap()
    )

    val sampleStatistics1 = ChartStatistics(
        min = 150.0,
        max = 3500.0,
        average = 1800.0,
        sum = 21600.0,
        isLoading = false
    )

    val sampleStatistics2 = ChartStatistics(
        min = 200.0,
        max = 2800.0,
        average = 1400.0,
        sum = 16800.0,
        isLoading = false
    )

    ComwattTheme {
        TimeSeriesStatisticsTable(
            timeSeriesList = listOf(sampleTimeSeries1, sampleTimeSeries2),
            statisticsList = listOf(sampleStatistics1, sampleStatistics2),
            colors = listOf(Color.Green, Color.Red),
            modifier = Modifier.padding(AppTheme.dimens.paddingNormal)
        )
    }
}

@HotPreviewLightDark
@Composable
fun DashboardStatisticsCardPreview() {
    val sampleStats = net.thevenot.comwatt.domain.model.SiteDailyData(
        totalProduction = 45123.2,
        totalConsumption = 38542.7,
        totalInjection = 11542.3,
        totalWithdrawals = 12325.4,
        selfConsumptionRate = 0.75,
        autonomyRate = 0.68
    )
    ComwattTheme {
        StatisticsCard(
            siteDailyData = sampleStats,
            totalsLabel = "Last 7 days",
            modifier = Modifier.padding(AppTheme.dimens.paddingNormal)
        )
    }
}
