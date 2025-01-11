package net.thevenot.comwatt.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.client.Session
import net.thevenot.comwatt.ui.home.gauge.SpeedTestScreen
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    session: Session,
    dataRepository: DataRepository,
    viewModel: HomeViewModel = viewModel { HomeViewModel(session, dataRepository) }
) {
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(uiState)
}

@Composable
private fun HomeScreenContent(
    uiState: HomeScreenState
) {
//    if(isLoading) {
//        Column(
//            modifier = Modifier.fillMaxSize(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center,
//        ) {
//            CircularProgressIndicator(
//                strokeCap = StrokeCap.Round,
//                color = MaterialTheme.colorScheme.onPrimary,
//                modifier = Modifier.size(24.dp)
//            )
//        }
//    }
//    else {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = if (uiState.production.isNaN()) "Loading..." else "Production: ${uiState.production}w",
            )
            Text(
                text = if (uiState.consumption.isNaN()) "Loading..." else "Consumption: ${uiState.consumption}w",
            )
            Text(
                text = if (uiState.injection.isNaN()) "Loading..." else "Injection: ${uiState.injection}w"
            )
            Text(
                text = if (uiState.withdrawals.isNaN()) "Loading..." else "Withdrawals: ${uiState.withdrawals}w"
            )
            Text(
                text = if (uiState.updateDate.isEmpty()) "Loading..." else "Update date: ${uiState.updateDate}"
            )
            Text(
                text = if (uiState.lastRefreshDate.isEmpty()) "Loading..." else "Last refresh: ${uiState.lastRefreshDate}"
            )
            Text(
                text = "Call number: ${uiState.callCount}",
            )
            Text(
                text = "Error number: ${uiState.errorCount}",
            )
            Spacer(modifier = Modifier.height(16.dp))
            SpeedTestScreen(uiState)
        }
//    }
}

@Composable
fun IconOnCircle(icon: ImageVector, circleColor: Color, circleSize: Int) {
    Box(
        modifier = Modifier.size(circleSize.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = circleColor,
                radius = size.minDimension / 2,
                style = Stroke(width = 4.dp.toPx()) // Adjust the stroke width as needed
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    ComwattTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            HomeScreenContent(
                HomeScreenState(
                    callCount = 123,
                    errorCount = 0,
                    isLoading = false,
                    production = 123.0,
                    consumption = 456.0,
                    injection = 789.0,
                    withdrawals = 951.0,
                    updateDate = "2021-09-01T12:00:00Z",
                    lastRefreshDate = "2021-09-01T12:00:00Z"
                )
            )
        }
    }
}