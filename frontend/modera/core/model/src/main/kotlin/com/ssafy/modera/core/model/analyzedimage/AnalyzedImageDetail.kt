package com.ssafy.modera.core.model.analyzedimage

data class AnalyzedImageDetail(
    val id: Long,
    val imageUrl: String,
    val thumbnailUrl: String,
    val title: String,
    val favorite: Boolean,
    val summary: String,
    val category: String,
    val tags: List<String>,
    val extractedTexts: List<String>,
    val keyInformation: List<String>,
    val isDocumented: Boolean,
    val isCalendared: Boolean,
    val updatedAt: Long,
)