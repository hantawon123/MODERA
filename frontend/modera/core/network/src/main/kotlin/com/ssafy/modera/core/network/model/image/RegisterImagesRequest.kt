package com.ssafy.modera.core.network.model.image

import kotlinx.serialization.Serializable

@Serializable
data class RegisterImagesRequest(
    val images: List<RegisterImageRequest>,
)
