package net.thevenot.comwatt.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.client.Session
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

    val callNumber by viewModel.callNumber.collectAsState()
    val errorNumber by viewModel.errorNumber.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val production by viewModel.production.collectAsState()
    val consumption by viewModel.consumption.collectAsState()
    val injection by viewModel.injection.collectAsState()
    val withdrawals by viewModel.withdrawals.collectAsState()
    val updateDate by viewModel.updateDate.collectAsState()
    val lastRefreshDate by viewModel.lastRefreshDate.collectAsState()

    HomeScreenContent(callNumber, errorNumber, isLoading, production, consumption, injection, withdrawals, updateDate, lastRefreshDate)
}

@Composable
private fun HomeScreenContent(
    callNumber: Int,
    errorNumber: Int,
    isLoading: Boolean,
    production: String,
    consumption: String,
    injection: String,
    withdrawals: String,
    updateDate: String,
    lastRefreshDate: String
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
                text = if (production.isEmpty()) "Loading..." else "Production: ${production}w",
            )
            Text(
                text = if (production.isEmpty()) "Loading..." else "Consumption: ${consumption}w",
            )
            Text(
                text = if (production.isEmpty()) "Loading..." else "Injection: ${injection}w"
            )
            Text(
                text = if (production.isEmpty()) "Loading..." else "Withdrawals: ${withdrawals}w"
            )
            Text(
                text = if (production.isEmpty()) "Loading..." else "Update date: $updateDate"
            )
            Text(
                text = if (production.isEmpty()) "Loading..." else "Last refresh: $lastRefreshDate"
            )
            Text(
                text = if (production.isEmpty()) "Loading..." else "Call number: $callNumber",
            )
            Text(
                text = if (production.isEmpty()) "Loading..." else "Error number: $errorNumber",
            )
        }
//    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    ComwattTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            HomeScreenContent(
                callNumber = 123,
                errorNumber = 0,
                isLoading = false,
                production = "123",
                consumption = "456",
                injection = "789",
                withdrawals = "951",
                updateDate = "2021-09-01T12:00:00Z",
                lastRefreshDate = "2021-09-01T12:00:00Z"
            )
        }
    }
}