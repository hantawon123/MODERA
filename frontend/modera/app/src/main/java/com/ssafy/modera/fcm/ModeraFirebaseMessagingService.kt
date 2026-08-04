package com.ssafy.modera.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.modera.sync.work.SyncResourceType
import com.ssafy.modera.sync.work.SyncWorkEnqueuer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ModeraFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var syncWorkEnqueuer: SyncWorkEnqueuer

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
}