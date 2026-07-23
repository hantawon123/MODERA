package com.ssafy.modera.core.network.model

import com.ssafy.modera.core.model.ImageAnalysisStatus

data class AnalyzedImagesRequest(
    val statuses: List<ImageAnalysisStatus> = emptyList(),
    val categoryId: Long? = null,
    val tagId: Long? = null,
    val favorite: Boolean? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    val statusQuery: String?
        get() = statuses
            .takeIf(List<ImageAnalysisStatus>::isNotEmpty)
            ?.joinToString(",") { status ->
                status.name
            }
}