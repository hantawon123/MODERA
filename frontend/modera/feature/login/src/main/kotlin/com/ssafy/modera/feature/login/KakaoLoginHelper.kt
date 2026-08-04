package com.ssafy.modera.feature.login

import android.content.Context
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient

private const val KAKAO_DEVELOPERS_LOG_TAG = "KakaoDevelopers"

internal fun startKakaoLogin(
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
