package net.thevenot.comwatt.ui.nav

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import org.jetbrains.compose.resources.stringResource

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        val currentBackStackEntry = navController.currentBackStackEntryAsState().value
        val currentDestination = currentBackStackEntry?.destination

        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                    )
                },
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
