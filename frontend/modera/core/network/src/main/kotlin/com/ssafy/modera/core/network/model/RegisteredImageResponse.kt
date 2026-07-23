package com.ssafy.modera.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisteredImageResponse(
    val clientRequestId: String,
    val imageId: Long,
    val fileName: String,
    val uploadUrl: String,
    val uploadExpiresIn: Int,
)
