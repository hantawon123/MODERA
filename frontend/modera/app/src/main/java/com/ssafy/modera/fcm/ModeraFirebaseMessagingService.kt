package com.ssafy.modera.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.modera.core.common.network.di.ApplicationScope
import com.ssafy.modera.core.data.repository.notification.PushTokenRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ModeraFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushTokenRepository: PushTokenRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        applicationScope.launch {
            runCatching {
                pushTokenRepository.registerPushToken(installationId)
            }.onSuccess {
                Log.d(TAG, "Push token registered, installationId=$installationId")
            }.onFailure { error ->
                Log.w(TAG, "Failed to register push token", error)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "FCM message received from=${message.from}")
        FcmMessageToast.show(this, message)
    }

    private companion object {
        const val TAG = "ModeraFcmOkhttp"
    }
}
