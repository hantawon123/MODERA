package com.ssafy.modera.core.data.repository.notification

import android.content.Context
import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.network.service.notification.NotificationClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DefaultPushTokenRepository @Inject constructor(
    private val notificationClient: NotificationClient,
    @ApplicationContext private val context: Context,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : PushTokenRepository {

    override suspend fun registerPushToken(fcmToken: String) {
        withContext(ioDispatcher) {
            notificationClient.updatePushToken(
                deviceId = getOrCreateDeviceId(),
                fcmToken = fcmToken,
            )
        }
    }

    override suspend fun deletePushToken() {
        withContext(ioDispatcher) {
            notificationClient.deletePushToken(
                deviceId = requireDeviceId(),
            )
        }
    }

    private fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also { deviceId ->
                prefs.edit()
                    .putString(KEY_DEVICE_ID, deviceId)
                    .apply()
            }
    }

    private fun requireDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_ID, null)
            ?: error("Device ID not found")
    }

    private companion object {
        const val PREFS_NAME = "modera_device"
        const val KEY_DEVICE_ID = "device_id"
    }
}
