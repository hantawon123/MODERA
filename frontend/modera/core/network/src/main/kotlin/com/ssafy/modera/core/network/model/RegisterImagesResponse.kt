package com.ssafy.modera.core.network.model

import com.ssafy.modera.core.model.RegisterImagesResult
import kotlinx.serialization.Serializable

@Serializable
data class RegisterImagesResponse(
    val registered: List<RegisteredImageResponse>,
    val duplicated: List<DuplicatedImageResponse>,
    val failed: List<FailedImageResponse>,
)

fun RegisterImagesResponse.asExternalModel(): RegisterImagesResult =
    RegisterImagesResult(
        registered = registered.map { it.asExternalModel() },
        duplicated = duplicated.map { it.asExternalModel() },
        failed = failed.map { it.asExternalModel() },
    )
