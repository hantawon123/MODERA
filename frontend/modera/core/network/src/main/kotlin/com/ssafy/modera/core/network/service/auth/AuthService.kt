package com.ssafy.modera.core.network.service.auth

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.auth.AuthTokenResponse
import com.ssafy.modera.core.network.model.auth.KakaoLoginRequest
import com.ssafy.modera.core.network.model.auth.LogoutRequest
import com.ssafy.modera.core.network.model.auth.LogoutResponse
import com.ssafy.modera.core.network.model.auth.RefreshTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

interface AuthService {
    @POST("api/v1/auth/kakao/login")
    suspend fun loginWithKakao(
        @Body request: KakaoLoginRequest,
    ): ApiResponse<BaseResponse<AuthTokenResponse>>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest,
    ): ApiResponse<BaseResponse<AuthTokenResponse>>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String,
        @Body request: LogoutRequest,
    ): ApiResponse<BaseResponse<LogoutResponse>>
}
