package com.ssafy.modera.core.network.model.image

import com.ssafy.modera.core.model.image.FailedImage
import kotlinx.serialization.Serializable

@Serializable
data class FailedImageResponse(
    val clientRequestId: String,
    val fileName: String,
    val reason: String,
)

fun FailedImageResponse.asExternalModel(): FailedImage =
    FailedImage(
        clientRequestId = clientRequestId,
        fileName = fileName,
        reason = reason,
    )
