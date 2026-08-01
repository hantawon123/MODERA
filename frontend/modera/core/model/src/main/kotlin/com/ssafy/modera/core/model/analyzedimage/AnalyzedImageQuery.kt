package com.ssafy.modera.core.model.analyzedimage

data class AnalyzedImageQuery(
    val categoryId: Long? = null,
    val favorite: Boolean? = null,
    val keyword: String? = null,
    val sort: String = UPLOADED_DESC,
) {
    companion object {
        const val UPLOADED_DESC = "UPLOADED_DESC"
    }
}