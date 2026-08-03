package com.ssafy.modera.core.data.repository.auth

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.datastore.AuthSession
import com.ssafy.modera.core.datastore.AuthSessionStore
import com.ssafy.modera.core.network.service.auth.AuthClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val authClient: AuthClient,
    private val authSessionStore: AuthSessionStore,
    @param:Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {
    override val session: Flow<AuthSession> = authSessionStore.session

    override suspend fun loginWithKakao(kakaoAccessToken: String) = withContext(ioDispatcher) {
        val deviceId = authSessionStore.getOrCreateDeviceId()
        val response = authClient.loginWithKakao(
            kakaoAccessToken = kakaoAccessToken,
            deviceId = deviceId,
        )
        authSessionStore.saveSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.userId,
            deviceId = deviceId,
        )
    }

    override suspend fun restoreSession() = withContext(ioDispatcher) {
        val currentSession = session.first()
        if (!currentSession.isAuthenticated) {
            return@withContext
        }

        runCatching {
            authClient.refreshToken(
                refreshToken = currentSession.refreshToken,
                deviceId = currentSession.deviceId,
            )
        }.onSuccess { response ->
            authSessionStore.saveSession(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                userId = response.userId,
                deviceId = currentSession.deviceId,
            )
        }.onFailure {
            authSessionStore.clearSession()
        }
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        val currentSession = session.first()
        try {
            if (currentSession.isAuthenticated) {
                authClient.logout(
                    accessToken = currentSession.accessToken,
                    refreshToken = currentSession.refreshToken,
                    deviceId = currentSession.deviceId,
                )
            }
        } finally {
            authSessionStore.clearSession()
        }
    }

    override suspend fun clearSession() {
        authSessionStore.clearSession()
    }
}
