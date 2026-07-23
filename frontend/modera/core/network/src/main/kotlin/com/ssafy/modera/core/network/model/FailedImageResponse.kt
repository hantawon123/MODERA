package com.ssafy.modera.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class FailedImageResponse(
    val clientRequestId: String,
    val fileName: String,
    val reason: String,
)
