package com.ssafy.modera.core.model

data class RegisterImagesResult(
    val registered: List<RegisteredImage>,
    val duplicated: List<DuplicatedImage>,
    val failed: List<FailedImage>,
)

data class RegisteredImage(
    val clientRequestId: String,
    val imageId: Long,
    val fileName: String,
    val uploadUrl: String,
    val uploadExpiresIn: Int,
)

data class DuplicatedImage(
    val clientRequestId: String,
    val fileName: String,
    val existingImageId: Long,
)

data class FailedImage(
    val clientRequestId: String,
    val fileName: String,
    val reason: String,
)
