package com.ssafy.modera.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterImagesRequest(
    val images: List<RegisterImageRequest>,
)
