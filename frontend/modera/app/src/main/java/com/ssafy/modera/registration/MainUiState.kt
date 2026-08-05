package com.ssafy.modera.registration

import com.ssafy.modera.core.model.image.DuplicatedImage
import com.ssafy.modera.core.model.image.FailedImage
import com.ssafy.modera.core.model.image.RegisteredImage
import com.ssafy.modera.core.model.image.SelectedImage

data class MainUiState(
    val selectedImages: List<SelectedImage> = emptyList(),
    val registeredImages: List<RegisteredImage> = emptyList(),
    val duplicatedImages: List<DuplicatedImage> = emptyList(),
    val failedImages: List<FailedImage> = emptyList(),
) {
    fun registerSummaryMessage(): String =
        "성공 ${registeredImages.size}장, " +
            "중복 ${duplicatedImages.size}장, " +
            "실패 ${failedImages.size}장"
}

fun MainUiState.startRegistration(imageCount: Int): MainUiState =
    copy(
        selectedImages = emptyList(),
        registeredImages = emptyList(),
        duplicatedImages = emptyList(),
        failedImages = emptyList(),
    )

fun MainUiState.applyRegistrationOutcome(
    outcome: ImageRegistrationOutcome,
): MainUiState =
    copy(
        selectedImages = selectedImages + outcome.processedImage,
        registeredImages = registeredImages + outcome.registered,
        duplicatedImages = duplicatedImages + outcome.duplicated,
        failedImages = failedImages + outcome.failed,
    )
