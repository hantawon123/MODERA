package com.ssafy.modera.core.network.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class KakaoLoginRequest(
    val kakaoAccessToken: String,
    val deviceId: String,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
    val deviceId: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
    val deviceId: String,
)

@Serializable
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Long,
)

@Serializable
data class LogoutResponse(
    val loggedOut: Boolean,
)
