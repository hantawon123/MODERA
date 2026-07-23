package com.ssafy.modera.core.network.model.image

import com.ssafy.modera.core.model.image.UploadCompleteResult
import kotlinx.serialization.Serializable

@Serializable
data class UploadCompleteResponse(
    val imageId: Long,
    val uploadCompleted: Boolean,
    val uploadedAt: String,
)

fun UploadCompleteResponse.asExternalModel(): UploadCompleteResult =
    UploadCompleteResult(
        imageId = imageId,
        uploadCompleted = uploadCompleted,
        uploadedAt = uploadedAt,
    )
