package com.ssafy.modera.core.model

data class AnalyzedImageDetail(
    val id: Long,
    val title: String,
    val imageUrl: String,
    val categoryId: Long,
    val categoryName: String,
    val uploadedAt: String,
    val hashtags: List<String>,
    val isFavorite: Boolean,
    val summary: String,
    val keyInfo: List<KeyInfoItem>,
    val ocrText: String,
)

data class KeyInfoItem(
    val label: String,
    val value: String,
)
