package net.thevenot.comwatt.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import net.thevenot.comwatt.DataRepository

@Composable
fun DashboardScreen(
    dataRepository: DataRepository,
    viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(dataRepository)
    }
) {
    val devices = viewModel.devices.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        devices.value.forEach {
            Text("Device: ${it.name}")
            Text("Kind: ${it.deviceKind?.code}")
        }
    }
}