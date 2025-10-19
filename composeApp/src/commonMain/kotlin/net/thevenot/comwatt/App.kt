package net.thevenot.comwatt

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import net.thevenot.comwatt.ui.dashboard.DashboardScreen
import net.thevenot.comwatt.ui.home.HomeScreen
import net.thevenot.comwatt.ui.login.LoginScreen
import net.thevenot.comwatt.ui.nav.Screen
import net.thevenot.comwatt.ui.settings.SettingsScreen
import net.thevenot.comwatt.ui.site.SiteChooserScreen
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.user.UserSettingsPanel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(
    appContainer: AppContainer,
    dynamicColor: Boolean = false
) {
    ComwattTheme(
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
    val snackbarHostState = remember { SnackbarHostState() }

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
        mainGraph(navController, dataRepository, viewModelStoreOwner, snackbarHostState)
        addUserSettingsDialog(navController, dataRepository)
    }
}

fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    dataRepository: DataRepository,
    viewModelStoreOwner: ViewModelStoreOwner,
    snackbarHostState: SnackbarHostState
) {
    navigation<Screen.Main>(
        startDestination = Screen.Home
    ) {
        composable<Screen.Home> {
            HomeScreen(
                navController = navController,
                snackbarHostState = snackbarHostState,
                dataRepository = dataRepository
            )
        }
        composable<Screen.Dashboard> {
            DashboardScreen(navController, snackbarHostState, dataRepository)
        }
        composable<Screen.Devices> {
            Text("Not Implemented Yet")
        }
        composable<Screen.More> {
            Text("Not Implemented Yet")
        }
        composable<Screen.Settings> {
            SettingsScreen(dataRepository)
        }
    }
}

fun NavGraphBuilder.addUserSettingsDialog(
    navController: NavController,
    dataRepository: DataRepository
) {
    composable<Screen.UserSettings>(
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(400)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(400)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(400)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(400)
            )
        }
    ) {
        UserSettingsPanel(
            dataRepository = dataRepository,
            onSettings = {
                navController.navigate(Screen.Settings)
            },
            onChangeSite = {
                navController.navigate(Screen.SiteChooser)
            },
            onLogout = {
                navController.navigate(Screen.Login) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onClose = {
                navController.popBackStack()
            }
        )
    }
}