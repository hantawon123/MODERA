package com.ssafy.modera.core.network.model.notification

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePushTokenRequest(
    val deviceId: String,
    val fcmToken: String,
)
