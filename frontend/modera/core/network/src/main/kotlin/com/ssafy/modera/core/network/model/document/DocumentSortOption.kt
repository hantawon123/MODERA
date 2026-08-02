package com.ssafy.modera.core.network.model.document

enum class DocumentSortOption(
    val value: String,
) {
    UPDATED_DESC(
        value = "UPDATED_DESC",
    ),
    UPDATED_ASC(
        value = "UPDATED_ASC",
    ),
    NAME_ASC(
        value = "NAME_ASC",
    ),
}