package com.ssafy.modera.core.network.service.notification

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.network.model.notification.UpdatePushTokenRequest
import com.ssafy.modera.core.network.model.notification.UpdatePushTokenResponse
import javax.inject.Inject

class NotificationClient @Inject constructor(
    private val notificationService: NotificationService,
) {
    suspend fun updatePushToken(
        deviceId: String,
        fcmToken: String,
    ): UpdatePushTokenResponse =
        notificationService
            .updatePushToken(
                UpdatePushTokenRequest(
                    deviceId = deviceId,
                    fcmToken = fcmToken,
                ),
            )
            .getOrThrow()
            .data

    suspend fun deletePushToken(
        deviceId: String,
    ): String =
        notificationService
            .deletePushToken(deviceId)
            .getOrThrow()
            .data
}
