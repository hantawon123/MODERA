package com.ssafy.modera.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class DuplicatedImageResponse(
    val clientRequestId: String,
    val fileName: String,
    val existingImageId: Long,
)
