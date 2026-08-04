package com.ssafy.modera.core.network.model.notification

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePushTokenResponse(
    val deviceId: String,
)
