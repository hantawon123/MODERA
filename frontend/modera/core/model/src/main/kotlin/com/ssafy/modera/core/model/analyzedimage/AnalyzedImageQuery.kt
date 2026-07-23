package com.ssafy.modera.core.model.analyzedimage

data class AnalyzedImageQuery(
    val statuses: Set<ImageAnalysisStatus> = emptySet(),
    val categoryId: Long? = null,
    val tagId: Long? = null,
    val favorite: Boolean? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
)