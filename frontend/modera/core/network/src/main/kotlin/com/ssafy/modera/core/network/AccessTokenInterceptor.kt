package com.ssafy.modera.core.network

import okhttp3.Interceptor
import okhttp3.Response

internal class AccessTokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = BuildConfig.ACCESS_TOKEN
        val request = if (token.isBlank()) {
            chain.request()
        } else {
            chain.request()
                .newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
