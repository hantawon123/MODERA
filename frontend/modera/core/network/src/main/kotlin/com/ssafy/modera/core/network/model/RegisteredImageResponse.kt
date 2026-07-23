package com.ssafy.modera.core.network.model

import com.ssafy.modera.core.model.RegisteredImage
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
