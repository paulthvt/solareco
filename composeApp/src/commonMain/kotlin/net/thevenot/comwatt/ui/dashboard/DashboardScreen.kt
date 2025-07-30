package net.thevenot.comwatt.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    snackbarHostState: SnackbarHostState,
    dataRepository: DataRepository,
    viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(FetchTimeSeriesUseCase(dataRepository), dataRepository)
    }
) {
    var showTimeSelectionDialog by remember { mutableStateOf(false) }

    NestedAppScaffold(
        navController = navController,
        snackbarHostState = snackbarHostState,
    ) {
        DashboardScreenContent(dataRepository, viewModel)

        if (showTimeSelectionDialog) {
            TimeSelectionDialog { showTimeSelectionDialog = false }
        }
    }
}

@Composable
fun TimeSelectionDialog(onDismiss: () -> Unit) {
    var selectedIndex by remember { mutableStateOf(0) }
    val options = listOf("Hour", "Day", "Week", "Month", "Year", "Custom")
    AlertDialog(
        onDismissRequest = onDismiss, title = {
            Text(stringResource(Res.string.gauge_dialog_title))
        }, confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.gauge_dialog_close_button))
            }
        }, text = {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                SingleChoiceSegmentedButtonRow {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            onClick = { selectedIndex = index },
                            selected = index == selectedIndex
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    )
}