package com.ssafy.modera.core.network

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
            request.url.host != API_HOST
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
        const val API_HOST = "i15d207.p.ssafy.io"
    }
}
