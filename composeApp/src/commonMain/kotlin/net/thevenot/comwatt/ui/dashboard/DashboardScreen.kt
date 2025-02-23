package net.thevenot.comwatt.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.gauge_dialog_close_button
import comwatt.composeapp.generated.resources.gauge_dialog_title
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchTimeSeriesUseCase
import net.thevenot.comwatt.ui.nav.NestedAppScaffold
import org.jetbrains.compose.resources.stringResource

@Composable
fun DashboardScreen(
    navController: NavController,
    dataRepository: DataRepository,
    viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(FetchTimeSeriesUseCase(dataRepository))
    }
) {
    var showTimeSelectionDialog by remember { mutableStateOf(false) }

    NestedAppScaffold(
        navController = navController,
        actionsContent = {
            IconButton(onClick = { showTimeSelectionDialog = true }) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = "calendar",
                )
            }
        }
    ) {
        DashboardScreenContent(dataRepository, viewModel)

        if (showTimeSelectionDialog) {
            TimeSelectionDialog { showTimeSelectionDialog = false }
        }
    }
}

@Composable
fun TimeSelectionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = {
            Text(stringResource(Res.string.gauge_dialog_title))
        }, confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.gauge_dialog_close_button))
            }
        }, text = {
            Text("Time selection dialog content goes here.")
        }
    )
}