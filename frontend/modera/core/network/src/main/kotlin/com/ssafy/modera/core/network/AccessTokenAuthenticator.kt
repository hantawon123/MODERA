package com.ssafy.modera.core.network

import com.ssafy.modera.core.datastore.AuthSessionStore
import com.ssafy.modera.core.network.service.auth.AuthClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

internal class AccessTokenAuthenticator @Inject constructor(
    private val authClient: AuthClient,
    private val authSessionStore: AuthSessionStore,
) : Authenticator {
    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount() >= MAX_RETRY_COUNT) return null

        return runBlocking {
            refreshMutex.withLock {
                val session = authSessionStore.session.first()
                if (!session.isAuthenticated) return@withLock null

                val requestToken = response.request.header(AUTHORIZATION_HEADER)
                    ?.removePrefix(BEARER_PREFIX)
                val latestToken = authSessionStore.currentAccessToken

                if (latestToken.isNotBlank() && latestToken != requestToken) {
                    return@withLock response.request.withBearerToken(latestToken)
                }

                runCatching {
                    authClient.refreshToken(
                        refreshToken = session.refreshToken,
                        deviceId = session.deviceId,
                    )
                }.fold(
                    onSuccess = { tokenResponse ->
                        authSessionStore.saveSession(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                            userId = tokenResponse.userId,
                            deviceId = session.deviceId,
                        )
                        response.request.withBearerToken(tokenResponse.accessToken)
                    },
                    onFailure = {
                        authSessionStore.clearSession()
                        null
                    },
                )
            }
        }
    }

    private fun Response.responseCount(): Int {
        var count = 1
        var priorResponse = priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }

    private fun Request.withBearerToken(token: String): Request = newBuilder()
        .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$token")
        .build()

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val MAX_RETRY_COUNT = 2
    }
}
