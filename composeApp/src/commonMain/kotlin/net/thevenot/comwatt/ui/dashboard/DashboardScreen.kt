package net.thevenot.comwatt.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LineAxis
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.dashboard_screen_title
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchTimeSeriesUseCase
import net.thevenot.comwatt.ui.common.CenteredTitleWithIcon
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
        DashboardScreenContent(dataRepository, snackbarHostState, viewModel)
    }
}