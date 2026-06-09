package net.thevenot.comwatt.ui.dashboard

import net.thevenot.comwatt.domain.model.ChartTimeSeries
import net.thevenot.comwatt.domain.model.TimeSeries
import net.thevenot.comwatt.domain.model.TimeSeriesTitle
import net.thevenot.comwatt.domain.model.TimeSeriesType
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardSortTest {

    private fun consumptionChart(
        name: String,
        sum: Double,
        hasConsumption: Boolean = true,
    ): ChartTimeSeries {
        val series = if (hasConsumption) {
            listOf(
                TimeSeries(
                    title = TimeSeriesTitle(name, null),
                    type = TimeSeriesType.CONSUMPTION,
                    values = emptyMap()
                )
            )
        } else {
            listOf(
                TimeSeries(
                    title = TimeSeriesTitle(name, null),
                    type = TimeSeriesType.PRODUCTION,
                    values = emptyMap()
                )
            )
        }
        val stats = if (hasConsumption) {
            listOf(ChartStatistics(0.0, 0.0, 0.0, sum, false))
        } else {
            emptyList()
        }
        return ChartTimeSeries(name = name, timeSeries = series, statistics = stats)
    }

    private val overview = consumptionChart("Overview", sum = 9999.0)

    @Test
    fun `NAME mode sorts devices A to Z and pins overview first`() {
        val charts = listOf(
            overview,
            consumptionChart("Zebra", 1.0),
            consumptionChart("alpha", 2.0),
        )

        val result = sortDashboardCharts(charts, DashboardSortMode.NAME)

        assertEquals(listOf("Overview", "alpha", "Zebra"), result.map { it.name })
    }

    @Test
    fun `CONSUMPTION_DESC sorts most consuming first, overview pinned`() {
        val charts = listOf(
            overview,
            consumptionChart("Low", 5.0),
            consumptionChart("High", 50.0),
            consumptionChart("Mid", 25.0),
        )

        val result = sortDashboardCharts(charts, DashboardSortMode.CONSUMPTION_DESC)

        assertEquals(listOf("Overview", "High", "Mid", "Low"), result.map { it.name })
    }

    @Test
    fun `CONSUMPTION_ASC sorts least consuming first, overview pinned`() {
        val charts = listOf(
            overview,
            consumptionChart("Low", 5.0),
            consumptionChart("High", 50.0),
            consumptionChart("Mid", 25.0),
        )

        val result = sortDashboardCharts(charts, DashboardSortMode.CONSUMPTION_ASC)

        assertEquals(listOf("Overview", "Low", "Mid", "High"), result.map { it.name })
    }

    @Test
    fun `charts without consumption series rank as zero and fall to bottom in DESC`() {
        val charts = listOf(
            overview,
            consumptionChart("NoData", 0.0, hasConsumption = false),
            consumptionChart("Real", 10.0),
        )

        val result = sortDashboardCharts(charts, DashboardSortMode.CONSUMPTION_DESC)

        assertEquals(listOf("Overview", "Real", "NoData"), result.map { it.name })
    }

    @Test
    fun `equal consumption ties broken by name`() {
        val charts = listOf(
            overview,
            consumptionChart("Beta", 10.0),
            consumptionChart("Alpha", 10.0),
        )

        val result = sortDashboardCharts(charts, DashboardSortMode.CONSUMPTION_DESC)

        assertEquals(listOf("Overview", "Alpha", "Beta"), result.map { it.name })
    }

    @Test
    fun `empty list returns empty`() {
        assertEquals(emptyList(), sortDashboardCharts(emptyList(), DashboardSortMode.NAME))
    }

    @Test
    fun `single overview-only list is unchanged`() {
        val charts = listOf(overview)
        assertEquals(listOf("Overview"), sortDashboardCharts(charts, DashboardSortMode.NAME).map { it.name })
    }
}
