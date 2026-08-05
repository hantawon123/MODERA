package com.ssafy.modera.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.modera.core.common.network.di.ApplicationScope
import com.ssafy.modera.core.data.repository.notification.PushTokenRepository
import com.ssafy.modera.sync.work.SyncResourceType
import com.ssafy.modera.sync.work.SyncWorkEnqueuer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ModeraFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var syncWorkEnqueuer: SyncWorkEnqueuer

    @Inject
    lateinit var pushTokenRepository: PushTokenRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onRegistered(
        installationId: String,
    ) {
        super.onRegistered(installationId)

        applicationScope.launch {
            try {
                val synced =
                    pushTokenRepository.registerPushToken(
                        installationId = installationId,
                    )

                if (synced) {
                    Log.d(
                        TAG,
                        "Push installation registered: $installationId",
                    )
                } else {
                    Log.d(
                        TAG,
                        "Push installation saved until authentication: $installationId",
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(
                    TAG,
                    "Failed to register push installation",
                    exception,
                )
            }
        }
    }


    override fun onMessageReceived(
        message: RemoteMessage,
    ) {
        super.onMessageReceived(message)

        val event = message.data
            .toDataChangedEvent()
            ?: return

        when (event.resource) {
            SyncResourceType.DOCUMENT -> {
                syncWorkEnqueuer.enqueue(
                    resource = event.resource,
                    resourceId = event.resourceId,
                )
            }

            SyncResourceType.IMAGE,
            SyncResourceType.CALENDAR,
                -> Unit
        }
    }

    override fun onNewToken(
        token: String,
    ) {
        super.onNewToken(token)

        // TODO FCM 토큰을 서버에 등록하는 API 호출
    }

    private companion object {
        const val TAG = "ModeraFcmOkhttp"
    }
}