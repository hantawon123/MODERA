package com.ssafy.modera.core.model

data class AnalyzedImageSummary(
    val id: Long,
    val title: String,
    val imageUrl: String,
    val hashtags: List<String>,
)
