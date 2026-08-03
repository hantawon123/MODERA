package com.ssafy.modera.feature.login

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.ssafy.modera.core.component.ModeraIconTextButton
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

private const val KAKAO_DEVELOPERS_LOG_TAG = "KakaoDevelopers"

@Composable
fun LoginRoute(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LoginScreen(
        onKakaoLoginClick = {
            startKakaoLogin(
                context = context,
                onSuccess = viewModel::loginWithKakao,
                onFailure = viewModel::showKakaoLoginError,
            )
        },
        isLoading = uiState == LoginUiState.Loading,
        errorMessage = (uiState as? LoginUiState.Error)?.message,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    onKakaoLoginClick: () -> Unit,
    isLoading: Boolean,
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

        Text(
            text = stringResource(R.string.login_tagline),
            style = ModeraTheme.typography.bodyR16,
            color = ModeraTheme.colors.gray400,
        )

        Spacer(modifier = Modifier.height(26.dp))

        Image(
            painter = painterResource(com.ssafy.modera.core.designsystem.R.drawable.img_modera_logo

            ),
            contentDescription = stringResource(R.string.login_mascot_description),
            modifier = Modifier.size(LoginScreenDefaults.MascotSize),
        )

        Spacer(modifier = Modifier.weight(LoginScreenDefaults.MiddleWeight))

        Image(
            painter = painterResource(R.drawable.img_login_mascot),
            contentDescription = stringResource(R.string.login_mascot_description),
            modifier = Modifier.size(LoginScreenDefaults.MascotSize),
        )

        Spacer(modifier = Modifier.height(32.dp))

        ModeraIconTextButton(
            text = stringResource(R.string.login_kakao_button),
            icon = painterResource(ModeraIcons.Kakao),
            onClick = onKakaoLoginClick,
            buttonColor = ModeraTheme.colors.kakaoYellow,
            contentColor = ModeraTheme.colors.brown,
            borderColor = ModeraTheme.colors.kakaoYellow,
        )
//        Button(
//            onClick = onKakaoLoginClick,
//            enabled = !isLoading,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(LoginScreenDefaults.ButtonHeight),
//            shape = RoundedCornerShape(LoginScreenDefaults.ButtonCornerRadius),
//            colors = ModeraButtonDefaults.buttonColors(
//                containerColor = ModeraTheme.colors.kakaoYellow,
//                contentColor = ModeraTheme.colors.black,
//                disabledContainerColor = ModeraTheme.colors.yellow500,
//                disabledContentColor = ModeraTheme.colors.gray700,
//            ),
//        ) {
//            if (isLoading) {
//                LoadingWheel(
//                    contentDescription = stringResource(R.string.login_in_progress),
//                    modifier = Modifier.size(24.dp),
//                )
//            } else {
//                Row(
//                    horizontalArrangement = Arrangement.spacedBy(12.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//                    Icon(
//                        painter = painterResource(ModeraIcons.Kakao),
//                        contentDescription = stringResource(R.string.login_kakao_icon_description),
//                        modifier = Modifier.size(24.dp),
//                        tint = ModeraTheme.colors.black,
//                    )
//                    Text(
//                        text = stringResource(R.string.login_kakao_button),
//                        style = ModeraTheme.typography.titleSB18,
//                    )
//                }
//            }
//        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.red,
            )
        }

        Spacer(modifier = Modifier.weight(LoginScreenDefaults.BottomWeight))
    }
}

private fun startKakaoLogin(
    context: Context,
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit,
) {
    logKakaoDevelopersConfig(context)

    val accountLogin: () -> Unit = {
        UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
            if (error != null || token == null) {
                onFailure()
            } else {
                onSuccess(token.accessToken)
            }
        }
    }

    if (!UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
        accountLogin()
        return
    }

    UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
        when {
            token != null -> onSuccess(token.accessToken)
            error is ClientError && error.reason == ClientErrorCause.Cancelled -> onFailure()
            else -> accountLogin()
        }
    }
}

/**
 * 카카오 디벨로퍼스(앱 설정)에 등록해야 하는 값을 Logcat에 출력한다.
 * 필터 태그: [KakaoDevelopers]
 */
private fun logKakaoDevelopersConfig(context: Context) {
    val nativeAppKey = runCatching { KakaoSdk.appKey }.getOrNull().orEmpty()
    val keyHash = Utility.getKeyHash(context)
    val packageName = context.packageName
    val redirectUri = if (nativeAppKey.isNotBlank()) {
        "kakao$nativeAppKey://oauth"
    } else {
        "kakao{NATIVE_APP_KEY}://oauth"
    }

    Log.d(KAKAO_DEVELOPERS_LOG_TAG, "========== Kakao Developers 등록 정보 ==========")
    Log.d(KAKAO_DEVELOPERS_LOG_TAG, "패키지명: $packageName")
    Log.d(KAKAO_DEVELOPERS_LOG_TAG, "네이티브 앱 키: $nativeAppKey")
    Log.d(KAKAO_DEVELOPERS_LOG_TAG, "키 해시: $keyHash")
    Log.d(KAKAO_DEVELOPERS_LOG_TAG, "Redirect URI: $redirectUri")
    Log.d(KAKAO_DEVELOPERS_LOG_TAG, "==============================================")
}

@Composable
private fun LoginBrand() {
    Box(
        modifier = Modifier
            .rotate(-3f)
            .background(ModeraTheme.colors.yellow500)
            .padding(horizontal = 28.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.login_brand),
            style = ModeraTheme.typography.titleB22.copy(
                fontStyle = FontStyle.Italic,
            ),
            color = ModeraTheme.colors.yellow700,
            modifier = Modifier.rotate(3f),
        )
    }
}

private object LoginScreenDefaults {
    val HorizontalPadding = 32.dp
    val MascotSize = 140.dp
    val ButtonHeight = 56.dp
    val ButtonCornerRadius = 18.dp
    const val TopWeight = 0.8f
    const val MiddleWeight = 1.7f
    const val BottomWeight = 0.55f
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LoginScreenPreview() {
    ModeraTheme {
        LoginScreen(
            onKakaoLoginClick = {},
            isLoading = false,
        )
    }
}
