package com.ssafy.modera.core.network

import okhttp3.HttpUrl.Companion.toHttpUrl

import com.ssafy.modera.core.datastore.AuthSessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

internal class AccessTokenInterceptor @Inject constructor(
    private val authSessionStore: AuthSessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = authSessionStore.currentAccessToken

        if (
            token.isBlank() ||
            (request.url.host != API_ORIGIN.host || request.url.port != API_ORIGIN.port || request.url.scheme != API_ORIGIN.scheme)
        ) {
            return chain.proceed(request)
        }

        return chain.proceed(
            request
                .newBuilder()
                .header("Authorization", "Bearer $token")
                .build(),
        )
    }

    private companion object {
        val API_ORIGIN = BuildConfig.API_BASE_URL.toHttpUrl()
    }
}
