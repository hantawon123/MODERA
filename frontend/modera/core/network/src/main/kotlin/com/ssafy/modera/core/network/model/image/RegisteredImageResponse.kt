package com.ssafy.modera.core.network.model.image

import com.ssafy.modera.core.model.image.RegisteredImage
import kotlinx.serialization.Serializable

@Serializable
data class RegisteredImageResponse(
    val clientRequestId: String,
    val imageId: Long,
    val fileName: String,
    val uploadUrl: String,
    val uploadExpiresIn: Int,
)

fun RegisteredImageResponse.asExternalModel(): RegisteredImage =
    RegisteredImage(
        clientRequestId = clientRequestId,
        imageId = imageId,
        fileName = fileName,
        uploadUrl = uploadUrl,
        uploadExpiresIn = uploadExpiresIn,
    )
