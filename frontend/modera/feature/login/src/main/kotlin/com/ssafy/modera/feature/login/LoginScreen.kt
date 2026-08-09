package com.ssafy.modera.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.component.ModeraIconTextButton
import com.ssafy.modera.core.designsystem.R.drawable.img_modera_logo
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.login.state.LoginError
import com.ssafy.modera.feature.login.state.LoginUiState
import com.ssafy.modera.feature.login.util.startKakaoLogin

@Composable
fun LoginRoute(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.resetUiState()
    }

    when (val state = uiState) {
        LoginUiState.Loading -> {
            LoadingScreen(modifier = modifier)
        }

        LoginUiState.Idle,
        is LoginUiState.Error,
        -> {
            LoginScreen(
                onKakaoLoginClick = {
                    startKakaoLogin(
                        context = context,
                        onSuccess = viewModel::loginWithKakao,
                        onFailure = viewModel::showKakaoLoginError,
                    )
                },
                errorMessage = (state as? LoginUiState.Error)?.type?.toMessage(),
                modifier = modifier,
            )
        }
    }
}

@Composable
fun LoginScreen(
    onKakaoLoginClick: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white)
            .padding(horizontal = LoginScreenDefaults.HorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(LoginScreenDefaults.TopWeight))

        LoginBrandSection()

        Spacer(modifier = Modifier.weight(LoginScreenDefaults.MiddleWeight))

        LoginActionSection(
            onKakaoLoginClick = onKakaoLoginClick,
            errorMessage = errorMessage,
        )

        Spacer(modifier = Modifier.weight(LoginScreenDefaults.BottomWeight))
    }
}

@Composable
private fun LoginBrandSection(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.login_tagline),
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray400,
        )
        Image(
            painter = painterResource(img_modera_logo),
            contentDescription = stringResource(R.string.login_brand),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LoginActionSection(
    onKakaoLoginClick: () -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.img_login_mascot),
            contentDescription = stringResource(R.string.login_mascot_description),
            modifier = Modifier.size(LoginScreenDefaults.MascotSize),
        )

        Spacer(modifier = Modifier.height(LoginScreenDefaults.MascotButtonSpacing))

        ModeraIconTextButton(
            text = stringResource(R.string.login_kakao_button),
            icon = painterResource(ModeraIcons.Kakao),
            onClick = onKakaoLoginClick,
            buttonColor = ModeraTheme.colors.kakaoYellow,
            contentColor = ModeraTheme.colors.black,
            borderColor = ModeraTheme.colors.kakaoYellow,
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(LoginScreenDefaults.ErrorSpacing))
            Text(
                text = errorMessage,
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.red,
            )
        }
    }
}

private object LoginScreenDefaults {
    val HorizontalPadding = 32.dp
    val MascotSize = 140.dp
    val MascotButtonSpacing = 20.dp
    val ErrorSpacing = 12.dp
    const val TopWeight = 1f
    const val MiddleWeight = 1.7f
    const val BottomWeight = 0.55f
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LoginScreenPreview() {
    ModeraTheme {
        LoginScreen(onKakaoLoginClick = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LoginScreenErrorPreview() {
    ModeraTheme {
        LoginScreen(
            onKakaoLoginClick = {},
            errorMessage = LoginError.ServerLoginFailed.toMessage(),
        )
    }
}

@Composable
private fun LoginError.toMessage(): String = when (this) {
    LoginError.KakaoLoginFailed -> stringResource(R.string.login_kakao_incomplete)
    LoginError.ServerLoginFailed -> stringResource(R.string.login_failed)
}
