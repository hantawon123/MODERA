package com.ssafy.modera.core.network.model

// Todo: core:model 모듈로 이동?
enum class CategorySortType(
    val queryValue: String,
) {
    NAME_ASC("name,asc"),
    UPDATED_AT_DESC("updatedAt,desc"),
    IMAGE_COUNT_DESC("imageCount,desc"),
}