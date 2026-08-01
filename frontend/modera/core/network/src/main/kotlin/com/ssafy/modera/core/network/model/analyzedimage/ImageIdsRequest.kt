package com.ssafy.modera.core.network.model.analyzedimage

import kotlinx.serialization.Serializable

@Serializable
data class ImageIdsRequest(
    val imageIds: List<Long>,
)