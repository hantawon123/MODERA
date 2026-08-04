package com.ssafy.modera.core.data.repository.notification

interface PushTokenRepository {
    suspend fun registerPushToken(fcmToken: String)

    suspend fun deletePushToken()
}
