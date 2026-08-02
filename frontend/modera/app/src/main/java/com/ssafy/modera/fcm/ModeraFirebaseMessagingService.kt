package com.ssafy.modera.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ModeraFirebaseMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        Log.d(TAG, "FCM registered, installationId=$installationId")
        // TODO: 서버에 Firebase Installation ID 등록 API 연동
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "FCM message received from=${message.from}")
        FcmMessageToast.show(this, message)
    }

    private companion object {
        const val TAG = "ModeraFcm"
    }
}
