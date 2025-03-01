package net.thevenot.comwatt.ui.user

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun UserSettingsDialog(navController: NavController) {
    AlertDialog(
        onDismissRequest = { navController.popBackStack() },
        title = { Text("User Settings") },
        text = { Text("Settings content goes here.") },
        confirmButton = {
            Button(onClick = { navController.popBackStack() }) {
                Text("OK")
            }
        }
    )
}