package com.ssafy.modera.core.network.model

import com.ssafy.modera.core.model.DuplicatedImage
import kotlinx.serialization.Serializable

@Serializable
data class DuplicatedImageResponse(
    val clientRequestId: String,
    val fileName: String,
    val existingImageId: Long,
)

fun DuplicatedImageResponse.asExternalModel(): DuplicatedImage =
    DuplicatedImage(
        clientRequestId = clientRequestId,
        fileName = fileName,
        existingImageId = existingImageId,
    )
