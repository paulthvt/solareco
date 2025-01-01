package net.thevenot.comwatt

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import net.thevenot.comwatt.client.Session
import net.thevenot.comwatt.ui.dashboard.DashboardScreen
import net.thevenot.comwatt.ui.home.HomeScreen
import net.thevenot.comwatt.ui.login.LoginScreen
import net.thevenot.comwatt.ui.nav.NestedAppScaffold
import net.thevenot.comwatt.ui.nav.Screen
import net.thevenot.comwatt.ui.nav.SessionNavType
import net.thevenot.comwatt.ui.site.SiteChooserScreen
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.reflect.typeOf

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
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainAppNavHost(appContainer)
        }
    }
}

@Composable
fun MainAppNavHost(appContainer: AppContainer, navController: NavHostController = rememberNavController()) {
    val dataRepository = appContainer.dataRepository

    NavHost(navController, startDestination = Screen.Login) {
        composable<Screen.Login> {
            LoginScreen(dataRepository) { session ->
                navController.navigate(Screen.SiteChooser(session)) {
                    popUpTo(Screen.Login) { inclusive = true }
                }
            }
        }
        composable<Screen.SiteChooser>(typeMap = mapOf(typeOf<Session?>() to SessionNavType)) { backStackEntry ->
            val mainParam: Screen.SiteChooser = backStackEntry.toRoute()
            mainParam.session?.let { session ->
                SiteChooserScreen(session, dataRepository) {
                    navController.navigate(Screen.Home(session)) {
                        popUpTo(Screen.SiteChooser(session)) { inclusive = true }
                    }
                }
            } ?: navController.navigate(Screen.Login)
        }
        mainGraph(navController, dataRepository)
    }
}

fun NavGraphBuilder.mainGraph(navController: NavHostController, dataRepository: DataRepository) {
    navigation<Screen.Main>(
        typeMap = mapOf(typeOf<Session?>() to SessionNavType),
        startDestination = Screen.Home()
    ) {
        composable<Screen.Home>(typeMap = mapOf(typeOf<Session?>() to SessionNavType)) { backStackEntry ->
            val mainParam: Screen.Home = backStackEntry.toRoute()
            mainParam.session?.let {
                NestedAppScaffold(navController, it) {
                    HomeScreen(it, dataRepository)
                }
            } ?: navController.navigate(Screen.Login)
        }
        composable<Screen.Dashboard>(typeMap = mapOf(typeOf<Session?>() to SessionNavType)) { backStackEntry ->
            val mainParam: Screen.Dashboard = backStackEntry.toRoute()
            mainParam.session?.let {
                NestedAppScaffold(navController, it) {
                    DashboardScreen()
                }
            } ?: navController.navigate(Screen.Login)
        }
        composable<Screen.Devices>(typeMap = mapOf(typeOf<Session?>() to SessionNavType)) { backStackEntry ->
            val mainParam: Screen.Devices = backStackEntry.toRoute()
            mainParam.session?.let {
                NestedAppScaffold(navController, it) {
                    HomeScreen(it, dataRepository)
                }
            } ?: navController.navigate(Screen.Login)
        }
        composable<Screen.More>(typeMap = mapOf(typeOf<Session?>() to SessionNavType)) { backStackEntry ->
            val mainParam: Screen.More = backStackEntry.toRoute()
            mainParam.session?.let {
                NestedAppScaffold(navController, it) {
                    HomeScreen(it, dataRepository)
                }
            } ?: navController.navigate(Screen.Login)
        }
    }
}
