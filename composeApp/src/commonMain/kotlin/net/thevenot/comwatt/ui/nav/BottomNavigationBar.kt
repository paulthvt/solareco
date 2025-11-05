package net.thevenot.comwatt.ui.nav

import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomNavigationBar(navController: NavController) {
    ShortNavigationBar {
        val currentBackStackEntry = navController.currentBackStackEntryAsState().value
        val currentDestination = currentBackStackEntry?.destination

        BottomNavItem.entries.forEach { item ->
            ShortNavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                    )
                },
                enabled = item.screen == Screen.Home || item.screen == Screen.Dashboard,
                label = { Text(stringResource(item.label)) },
                selected = currentDestination?.hierarchy?.any {
                    it.hasRoute(item.screen::class)
                } == true,
                onClick = {
                    navController.navigate(item.screen) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
