package net.thevenot.comwatt.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import net.thevenot.comwatt.client.Session

@Composable
fun NestedAppScaffold(
    navController: NavController,
    session: Session?,
    fab: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
//            TopNavigationBar(onClickLogout = {
//                loginViewModel.logout()
//                loginViewModel.clear()
//                navController.navigate(Screen.Login) {
//                    popUpTo(navController.graph.startDestinationId) {
//                        inclusive = true
//                    }
//                }
//            })
        },
        floatingActionButton = fab,
        bottomBar = { BottomNavigationBar(navController, session) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}
