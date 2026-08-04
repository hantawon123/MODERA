package com.ssafy.modera.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auth_session",
)

data class AuthSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: Long? = null,
    val deviceId: String = "",
) {
    val isAuthenticated: Boolean
        get() = accessToken.isNotBlank() && refreshToken.isNotBlank()
}

@Singleton
class AuthSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.authSessionDataStore

    @Volatile
    var currentAccessToken: String = ""
        private set

    val session: Flow<AuthSession> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map(::toAuthSession)
        .onEach { session ->
            currentAccessToken = session.accessToken
        }

    suspend fun getOrCreateDeviceId(): String {
        val currentSession = sessionSnapshot()
        if (currentSession.deviceId.isNotBlank()) {
            return currentSession.deviceId
        }

        val deviceId = UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            preferences[Keys.DeviceId] = deviceId
        }
        return deviceId
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        deviceId: String,
    ) {
        currentAccessToken = accessToken
        dataStore.edit { preferences ->
            preferences[Keys.AccessToken] = accessToken
            preferences[Keys.RefreshToken] = refreshToken
            preferences[Keys.UserId] = userId
            preferences[Keys.DeviceId] = deviceId
        }
    }

    suspend fun clearSession() {
        currentAccessToken = ""
        dataStore.edit { preferences ->
            val deviceId = preferences[Keys.DeviceId]
            preferences.clear()
            if (deviceId != null) {
                preferences[Keys.DeviceId] = deviceId
            }
        }
    }

    private suspend fun sessionSnapshot(): AuthSession =
        session.first()

    private fun toAuthSession(preferences: Preferences): AuthSession = AuthSession(
        accessToken = preferences[Keys.AccessToken].orEmpty(),
        refreshToken = preferences[Keys.RefreshToken].orEmpty(),
        userId = preferences[Keys.UserId],
        deviceId = preferences[Keys.DeviceId].orEmpty(),
    )

    private object Keys {
        val AccessToken = stringPreferencesKey("access_token")
        val RefreshToken = stringPreferencesKey("refresh_token")
        val UserId = longPreferencesKey("user_id")
        val DeviceId = stringPreferencesKey("device_id")
    }
}
