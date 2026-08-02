package com.ssafy.modera.core.model.analyzedimage

enum class AnalyzedImageSortType(
    val label: String,
    val queryValue: String,
) {
    UPLOADED_DESC(
        label = "최신 업로드순",
        queryValue = "UPLOADED_DESC",
    ),
    UPLOADED_ASC(
        label = "오래된 순",
        queryValue = "UPLOADED_ASC",
    ),
    TITLE_ASC(
        label = "사전순",
        queryValue = "TITLE_ASC",
    ),
}
