package com.ssafy.modera.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterImagesResponse(
    val registered: List<RegisteredImageResponse>,
    val duplicated: List<DuplicatedImageResponse>,
    val failed: List<FailedImageResponse>,
)
