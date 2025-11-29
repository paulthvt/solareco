package net.thevenot.comwatt.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Preview for the Consumption Widget
 *
 * Note: This is a regular Compose preview, not a Glance widget preview.
 * Glance widget previews require special configuration.
 * This preview shows approximately how the widget will look.
 */
@Preview(widthDp = 320, heightDp = 150, showBackground = true)
@Composable
fun ConsumptionWidgetPreview() {
    // Create sample data for preview
    val sampleData = WidgetConsumptionData(
        timestamps = List(12) { System.currentTimeMillis() - (11 - it) * 5 * 60 * 1000 },
        consumptions = listOf(
            1200.0,
            1350.0,
            1500.0,
            1400.0,
            1600.0,
            1800.0,
            1700.0,
            1900.0,
            2100.0,
            1950.0,
            1850.0,
            1750.0
        ),
        lastUpdateTime = System.currentTimeMillis(),
        maxConsumption = 2100.0,
        averageConsumption = 1683.33
    )

    // Create simple ASCII chart
    fun createChart(data: WidgetConsumptionData): String {
        if (data.consumptions.isEmpty()) return ""
        val maxValue = data.maxConsumption
        if (maxValue == 0.0) return ""
        val height = 8
        val blocks = listOf("▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")
        return data.consumptions.takeLast(40).joinToString("") { value ->
            val normalized = (value / maxValue * (height - 1)).toInt().coerceIn(0, height - 1)
            blocks[normalized]
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Consumption",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Statistics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Column {
                        Text(
                            text = "Current",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBBBBBB)
                        )
                        Text(
                            text = "${sampleData.consumptions.lastOrNull()?.toInt() ?: 0} W",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Average",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBBBBBB)
                        )
                        Text(
                            text = "${sampleData.averageConsumption.toInt()} W",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Peak",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBBBBBB)
                        )
                        Text(
                            text = "${sampleData.maxConsumption.toInt()} W",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chart
                Text(
                    text = createChart(sampleData),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Last update
                Text(
                    text = "Updated: Now",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF888888)
                )
            }
        }
    }
}
