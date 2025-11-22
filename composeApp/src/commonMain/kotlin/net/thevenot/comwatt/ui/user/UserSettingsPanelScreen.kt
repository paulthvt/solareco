package net.thevenot.comwatt.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.user_settings_panel_change_site
import comwatt.composeapp.generated.resources.user_settings_panel_close_button_content_description
import comwatt.composeapp.generated.resources.user_settings_panel_github_repo
import comwatt.composeapp.generated.resources.user_settings_panel_logout_button
import comwatt.composeapp.generated.resources.user_settings_panel_report_issue
import comwatt.composeapp.generated.resources.user_settings_panel_settings
import comwatt.composeapp.generated.resources.user_settings_panel_user_avatar_content_description
import comwatt.composeapp.generated.resources.user_settings_panel_version
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.getAppVersion
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun UserSettingsPanel(
    dataRepository: DataRepository,
    onLogout: () -> Unit = {},
    onChangeSite: () -> Unit = {},
    onSettings: () -> Unit = {},
    onClose: () -> Unit = {},
    viewModel: UserSettingsPanelViewModel = viewModel {
        UserSettingsPanelViewModel(dataRepository = dataRepository)
    }
) {
    LaunchedEffect(Unit) {
        viewModel.loadSite()
    }
    val uiState by viewModel.uiState.collectAsState()

    UserSettingsPanelScreenContent(
        uiState = uiState,
        onLogoutClick = {
            viewModel.logout {
                onLogout()
            }
        },
        onChangeSiteClick = {
            viewModel.clearSite {
                onChangeSite()
            }
        },
        onSettingsClick = onSettings,
        onClose = onClose
    )
}

@Composable
private fun UserSettingsPanelScreenContent(
    uiState: UserSettingsPanelState,
    onLogoutClick: () -> Unit = {},
    onChangeSiteClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars)
                .clickable(enabled = false) { /* Prevent closing when clicking inside */ },
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = onClose
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.user_settings_panel_close_button_content_description)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.dimens.paddingSmall))

                UserProfileSection(
                    uiState = uiState,
                    onLogout = onLogoutClick
                )
                Spacer(modifier = Modifier.height(24.dp))
                SettingsCard(
                    onChangeSiteClick = onChangeSiteClick,
                    onSettingsClick = onSettingsClick
                )

                Spacer(modifier = Modifier.weight(1f))

                VersionAndLinksSection()

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun UserProfileSection(
    uiState: UserSettingsPanelState,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = stringResource(Res.string.user_settings_panel_user_avatar_content_description),
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(AppTheme.dimens.paddingSmall))
            Text(
                text = uiState.userName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(AppTheme.dimens.paddingSmall))
            Text(
                text = uiState.siteName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(AppTheme.dimens.paddingSmall))
            Text(stringResource(Res.string.user_settings_panel_logout_button))
        }
    }
}

@Composable
private fun SettingsCard(
    onChangeSiteClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsMenuItem(
                icon = Icons.Default.SwapHoriz,
                title = stringResource(Res.string.user_settings_panel_change_site),
                onClick = onChangeSiteClick
            )

            HorizontalDivider()

            SettingsMenuItem(
                icon = Icons.Default.Settings,
                title = stringResource(Res.string.user_settings_panel_settings),
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(AppTheme.dimens.paddingNormal))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VersionAndLinksSection() {
    val uriHandler = LocalUriHandler.current
    val version = getAppVersion()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextButton(
            onClick = {
                uriHandler.openUri("https://github.com/paulthvt/solareco/releases/tag/$version")
            },
            shapes = ButtonDefaults.shapes()
        ) {
            Text(
                text = stringResource(Res.string.user_settings_panel_version, version),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    uriHandler.openUri("https://github.com/pthevenot/comwatt")
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(AppTheme.dimens.paddingExtraSmall))
                Text(
                    text = stringResource(Res.string.user_settings_panel_github_repo),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextButton(
                onClick = {
                    uriHandler.openUri("https://github.com/pthevenot/comwatt/issues/new")
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(AppTheme.dimens.paddingExtraSmall))
                Text(
                    text = stringResource(Res.string.user_settings_panel_report_issue),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
@Preview
fun PreviewUserSettingsDialog() {
    ComwattTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            UserSettingsPanelScreenContent(
                uiState = UserSettingsPanelState(
                    siteName = "My Site",
                    userName = "John Doe"
                ),
            )
        }
    }
}