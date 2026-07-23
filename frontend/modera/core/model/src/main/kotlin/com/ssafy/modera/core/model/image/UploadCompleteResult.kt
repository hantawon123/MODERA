package com.ssafy.modera.core.model.image

data class UploadCompleteResult(
    val imageId: Long,
    val uploadCompleted: Boolean,
    val uploadedAt: String,
)
