package com.ssafy.modera.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.component.ModeraConfirmDialog
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.settings.component.SettingsMenuItem

@Composable
fun SettingsRoute(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        SettingsUiState.Loading -> {
            LoadingScreen(modifier = modifier)
        }

        is SettingsUiState.Success -> {
            SettingsScreen(
                email = state.email,
                appVersion = state.appVersion,
                onBackClick = onBackClick,
                onLogoutClick = onLogoutClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun SettingsScreen(
    email: String,
    appVersion: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    onVersionInfoClick: () -> Unit = {},
    onOpenSourceLicenseClick: () -> Unit = {},
    onTermsAndPoliciesClick: () -> Unit = {},
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white)
            .padding(bottom = 30.dp),
    ) {
        ModeraTopBar(
            onBackClick = onBackClick,
            centerContent = {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = ModeraTheme.typography.bodySB16,
                    color = ModeraTheme.colors.gray900,
                    maxLines = 1,
                )
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsAccountSection(
                email = email,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.settings_app_section),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.gray400,
            )

            SettingsMenuItem(
                iconRes = ModeraIcons.InformationCircle,
                title = stringResource(R.string.settings_version_info),
                subtitle = if (appVersion.isNotBlank()) {
                    stringResource(R.string.settings_version_update_available)
                } else {
                    null
                },
                onClick = onVersionInfoClick,
            )

            SettingsMenuItem(
                iconRes = ModeraIcons.StarOutlined,
                title = stringResource(R.string.settings_open_source_license),
                onClick = onOpenSourceLicenseClick,
            )

            SettingsMenuItem(
                iconRes = ModeraIcons.Policy,
                title = stringResource(R.string.settings_terms_and_policies),
                onClick = onTermsAndPoliciesClick,
            )
        }

        Text(
            text = stringResource(R.string.settings_logout),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = { showLogoutDialog = true })
                .padding(vertical = 24.dp),
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray700,
            textDecoration = TextDecoration.Underline,
        )
    }

    if (showLogoutDialog) {
        ModeraConfirmDialog(
            icon = painterResource(ModeraIcons.Logout),
            title = stringResource(R.string.settings_logout_dialog_title),
            confirmText = stringResource(R.string.settings_logout_dialog_confirm),
            dismissText = stringResource(R.string.settings_logout_dialog_cancel),
            confirmButtonColor = ModeraTheme.colors.gray900,
            onConfirm = {
                showLogoutDialog = false
                onLogoutClick()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}

@Composable
private fun SettingsAccountSection(
    email: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(ModeraIcons.UserProfile),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = ModeraTheme.colors.gray300,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = stringResource(R.string.settings_my_account),
                style = ModeraTheme.typography.bodySB16,
                color = ModeraTheme.colors.gray900,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email.ifBlank {
                    stringResource(R.string.settings_email_unavailable)
                },
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray400,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun SettingsScreenPreview() {
    ModeraTheme {
        SettingsScreen(
            email = "jlkim909@kakao.com",
            appVersion = "1.0",
            onBackClick = {},
            onLogoutClick = {},
        )
    }
}
