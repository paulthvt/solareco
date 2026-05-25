package net.thevenot.comwatt.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.thevenot.comwatt.R

@Composable
private fun WidgetPreview(widgetData: WidgetConsumptionData) {
    val consumption = widgetData.consumptions.lastOrNull()?.toInt() ?: 0
    val production = widgetData.productions.lastOrNull()?.toInt() ?: 0
    val available = (production - consumption).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_bolt),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            BasicText(
                "Energy Overview",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        if (widgetData.consumptions.isNotEmpty() || widgetData.productions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (widgetData.consumptions.isNotEmpty()) {
                    PreviewPowerStat(R.drawable.ic_arrow_down_consumption, "${consumption}W")
                }
                if (widgetData.productions.isNotEmpty()) {
                    PreviewPowerStat(R.drawable.ic_arrow_up_production, "${production}W")
                }
                if (widgetData.consumptions.isNotEmpty() && widgetData.productions.isNotEmpty() && available > 0) {
                    PreviewPowerStat(R.drawable.ic_solar_available, "${available}W")
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                BasicText(
                    "Last update: 14:30",
                    style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    "No data available",
                    style = TextStyle(fontSize = 14.sp, color = Color.Gray)
                )
            }
        }
    }
}

@Composable
private fun PreviewPowerStat(iconRes: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp)
        )
        BasicText(
            text,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Preview(widthDp = 320, heightDp = 180)
@Composable
private fun PreviewWidgetWithSolarAvailable() {
    WidgetPreview(
        WidgetConsumptionData(
            timestamps = listOf(1L, 2L, 3L, 4L, 5L),
            consumptions = listOf(800.0, 850.0, 900.0, 750.0, 600.0),
            productions = listOf(1200.0, 1300.0, 1400.0, 1100.0, 1500.0),
            lastUpdateTime = System.currentTimeMillis(),
            maxConsumption = 900.0,
            averageConsumption = 780.0,
            maxProduction = 1500.0,
            averageProduction = 1300.0
        )
    )
}

@Preview(widthDp = 320, heightDp = 180)
@Composable
private fun PreviewWidgetNoSolarAvailable() {
    WidgetPreview(
        WidgetConsumptionData(
            timestamps = listOf(1L, 2L, 3L, 4L, 5L),
            consumptions = listOf(1200.0, 1300.0, 1500.0, 1400.0, 1800.0),
            productions = listOf(400.0, 500.0, 300.0, 450.0, 350.0),
            lastUpdateTime = System.currentTimeMillis(),
            maxConsumption = 1800.0,
            averageConsumption = 1440.0,
            maxProduction = 500.0,
            averageProduction = 400.0
        )
    )
}

@Preview(widthDp = 320, heightDp = 180)
@Composable
private fun PreviewWidgetConsumptionOnly() {
    WidgetPreview(
        WidgetConsumptionData(
            timestamps = listOf(1L, 2L, 3L, 4L, 5L),
            consumptions = listOf(800.0, 850.0, 900.0, 750.0, 1000.0),
            productions = emptyList(),
            lastUpdateTime = System.currentTimeMillis(),
            maxConsumption = 1000.0,
            averageConsumption = 860.0,
            maxProduction = 0.0,
            averageProduction = 0.0
        )
    )
}

@Preview(widthDp = 320, heightDp = 180)
@Composable
private fun PreviewWidgetEmpty() {
    WidgetPreview(WidgetConsumptionData.empty())
}
