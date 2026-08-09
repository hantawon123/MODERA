package com.ssafy.modera.core.network.service.auth

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.network.model.auth.AuthTokenResponse
import com.ssafy.modera.core.network.model.auth.KakaoLoginRequest
import com.ssafy.modera.core.network.model.auth.LogoutRequest
import com.ssafy.modera.core.network.model.auth.RefreshTokenRequest
import javax.inject.Inject

class AuthClient @Inject constructor(
    private val authService: AuthService,
) {
    suspend fun loginWithKakao(
        kakaoAccessToken: String,
        deviceId: String,
    ): AuthTokenResponse = authService
        .loginWithKakao(
            KakaoLoginRequest(
                kakaoAccessToken = kakaoAccessToken,
                deviceId = deviceId,
            ),
        )
        .getOrThrow()
        .data

    suspend fun refreshToken(
        refreshToken: String,
        deviceId: String,
    ): AuthTokenResponse = authService
        .refreshToken(
            RefreshTokenRequest(
                refreshToken = refreshToken,
                deviceId = deviceId,
            ),
        )
        .getOrThrow()
        .data

    suspend fun logout(
        accessToken: String,
        refreshToken: String,
        deviceId: String,
    ) {
        authService.logout(
            authorization = "Bearer $accessToken",
            request = LogoutRequest(
                refreshToken = refreshToken,
                deviceId = deviceId,
            ),
        ).getOrThrow()
    }
}
