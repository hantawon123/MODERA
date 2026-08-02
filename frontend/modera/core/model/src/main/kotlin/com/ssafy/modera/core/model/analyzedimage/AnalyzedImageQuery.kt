package com.ssafy.modera.core.model.analyzedimage

data class AnalyzedImageQuery(
    val categoryId: Long? = null,
    val favorite: Boolean? = null,
    val keyword: String? = null,
    val sort: AnalyzedImageSortType = AnalyzedImageSortType.UPLOADED_DESC,
)