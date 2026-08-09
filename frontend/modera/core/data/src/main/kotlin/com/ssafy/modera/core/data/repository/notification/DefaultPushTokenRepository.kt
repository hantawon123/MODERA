package com.ssafy.modera.core.data.repository.notification

import android.content.Context
import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.datastore.AuthSessionStore
import com.ssafy.modera.core.network.service.notification.NotificationClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPushTokenRepository @Inject constructor(
    private val notificationClient: NotificationClient,
    private val authSessionStore: AuthSessionStore,
    @ApplicationContext private val context: Context,
    @param:Dispatcher(ModeraDispatcher.IO)
    private val ioDispatcher: CoroutineDispatcher,
) : PushTokenRepository {

    override suspend fun registerPushToken(
        installationId: String,
    ): Boolean = withContext(ioDispatcher) {
        saveInstallationId(
            installationId = installationId,
        )

        syncPushTokenInternal()
    }

    override suspend fun syncPushToken(): Boolean =
        withContext(ioDispatcher) {
            syncPushTokenInternal()
        }

    override suspend fun deletePushToken() {
        withContext(ioDispatcher) {
            val session = authSessionStore.session.first()

            if (!session.isAuthenticated) {
                return@withContext
            }

            notificationClient.deletePushToken(
                deviceId = session.deviceId,
            )
        }
    }

    private suspend fun syncPushTokenInternal(): Boolean {
        val installationId =
            getInstallationId() ?: return false

        val session = authSessionStore.session.first()

        if (!session.isAuthenticated) {
            return false
        }

        notificationClient.updatePushToken(
            deviceId = session.deviceId,
            fcmToken = installationId,
        )

        return true
    }

    private fun saveInstallationId(
        installationId: String,
    ) {
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            )
            .edit()
            .putString(
                KEY_INSTALLATION_ID,
                installationId,
            )
            .apply()
    }

    private fun getInstallationId(): String? =
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            )
            .getString(
                KEY_INSTALLATION_ID,
                null,
            )

    private companion object {
        const val PREFS_NAME = "modera_push"
        const val KEY_INSTALLATION_ID = "installation_id"
    }
}