package com.ssafy.modera.core.data.repository.auth

import com.ssafy.modera.core.datastore.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<AuthSession>

    suspend fun loginWithKakao(kakaoAccessToken: String)

    suspend fun restoreSession()

    suspend fun logout()

    suspend fun clearSession()
}
