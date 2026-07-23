package com.ssafy.modera.core.model.category

enum class CategorySortType(
    val queryValue: String,
) {
    NAME_ASC("name,asc"),
    UPDATED_AT_DESC("updatedAt,desc"),
    IMAGE_COUNT_DESC("imageCount,desc"),
}