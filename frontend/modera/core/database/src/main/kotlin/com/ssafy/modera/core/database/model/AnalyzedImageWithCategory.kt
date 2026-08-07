package com.ssafy.modera.core.database.model

import androidx.room.Embedded
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail

data class AnalyzedImageWithCategory(
    @Embedded
    val analyzedImage: AnalyzedImageEntity,
    @Embedded(prefix = "category_")
    val category: CategoryEntity,
)

fun AnalyzedImageWithCategory.asExternalModel(
    hasRelatedDocuments: Boolean,
): AnalyzedImageDetail =
    AnalyzedImageDetail(
        id = analyzedImage.imageId,
        imageUrl = analyzedImage.imageUrl.orEmpty(),
        thumbnailUrl = analyzedImage.thumbnailUrl,
        title = analyzedImage.title,
        favorite = analyzedImage.favorite,
        summary = analyzedImage.summary,
        category = category.name,
        tags = analyzedImage.tags,
        extractedTexts = analyzedImage.ocrRefinedText.toExtractedTexts(),
        keyInformation = analyzedImage.keyInformation,
        isDocumented = hasRelatedDocuments,
        isCalendared = analyzedImage.isCalendared,
        updatedAt = analyzedImage.updatedAt,
    )

private fun String.toExtractedTexts(): List<String> =
    split(Regex("\\s+"))
        .map(String::trim)
        .filter(String::isNotBlank)
        .filter { text ->
            text.any(Char::isLetterOrDigit)
        }