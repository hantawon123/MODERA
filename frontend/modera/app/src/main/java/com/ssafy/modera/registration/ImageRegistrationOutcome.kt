package com.ssafy.modera.registration

import com.ssafy.modera.core.model.image.DuplicatedImage
import com.ssafy.modera.core.model.image.FailedImage
import com.ssafy.modera.core.model.image.RegisteredImage
import com.ssafy.modera.core.model.image.SelectedImage

data class ImageRegistrationOutcome(
    val processedImage: SelectedImage,
    val registered: List<RegisteredImage> = emptyList(),
    val duplicated: List<DuplicatedImage> = emptyList(),
    val failed: List<FailedImage> = emptyList(),
)
