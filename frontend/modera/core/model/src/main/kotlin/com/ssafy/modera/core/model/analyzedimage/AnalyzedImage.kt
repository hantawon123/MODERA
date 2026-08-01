package com.ssafy.modera.core.model.analyzedimage

data class AnalyzedImage(
    val id: Long,
    val title: String,
    val summary: String,
    val thumbnailUrl: String,
    val hashtags: List<String>,
    val favorite: Boolean = false,
    val isDocumented: Boolean = false,
    val hasSchedule: Boolean = false,
)