package net.thevenot.comwatt

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import net.thevenot.comwatt.domain.FetchSiteTimeSeriesUseCase
import net.thevenot.comwatt.ui.dashboard.DashboardScreen
import net.thevenot.comwatt.ui.home.HomeScreen
import net.thevenot.comwatt.ui.home.HomeViewModel
import net.thevenot.comwatt.ui.login.LoginScreen
import net.thevenot.comwatt.ui.nav.NestedAppScaffold
import net.thevenot.comwatt.ui.nav.Screen
import net.thevenot.comwatt.ui.site.SiteChooserScreen
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    appContainer: AppContainer
) {
    ComwattTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            MainAppNavHost(appContainer)
        }
    }
}

@Composable
fun MainAppNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val dataRepository = appContainer.dataRepository
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }

    NavHost(navController, startDestination = Screen.Login) {
        composable<Screen.Login> {
            LoginScreen(dataRepository) {
                navController.navigate(Screen.SiteChooser) {
                    popUpTo(Screen.Login) { inclusive = true }
                }
            }
        }
        composable<Screen.SiteChooser> {
            SiteChooserScreen(dataRepository) {
                navController.navigate(Screen.Main) {
                    popUpTo(Screen.SiteChooser) { inclusive = true }
                }
            }
        }
        mainGraph(navController, dataRepository, viewModelStoreOwner)
    }
}

fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    dataRepository: DataRepository,
    viewModelStoreOwner: ViewModelStoreOwner
) {
    navigation<Screen.Main>(
        startDestination = Screen.Home
    ) {
        composable<Screen.Home> {
            NestedAppScaffold(navController) {
                HomeScreen(
                    dataRepository = dataRepository,
                    viewModel = viewModel(viewModelStoreOwner = viewModelStoreOwner) {
                        HomeViewModel(FetchSiteTimeSeriesUseCase(dataRepository))
                    })
            }

        }
        composable<Screen.Dashboard> {
            NestedAppScaffold(navController) {
                DashboardScreen()
            }
        }
        composable<Screen.Devices> {
            NestedAppScaffold(navController) {
                DashboardScreen()
            }
        }
        composable<Screen.More> {
            NestedAppScaffold(navController) {
                DashboardScreen()
            }
        }
    }
}
