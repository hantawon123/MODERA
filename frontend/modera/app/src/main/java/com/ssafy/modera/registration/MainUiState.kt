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
    fun duplicatedSummaryMessage(): String? =
        duplicatedImages.size.takeIf { it > 0 }?.let { count ->
            "${count}장의 사진은 이미 업로드 되어있습니다"
        }

    fun failedSummaryMessage(): String? =
        failedImages.size.takeIf { it > 0 }?.let { count ->
            "${count}장의 사진 업로드에 실패했습니다"
        }
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
