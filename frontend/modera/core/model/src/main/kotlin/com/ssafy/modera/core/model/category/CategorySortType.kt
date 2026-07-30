package com.ssafy.modera.core.model.category

enum class CategorySortType(
    val label: String,
    val queryValue: String,
) {
    NAME_ASC(label = "이름순", queryValue = "name,asc"),
    UPDATED_AT_ASC(label = "최신 순", queryValue = "updatedAt,asc"),
    UPDATED_AT_DESC(label = "오래된 순", queryValue = "updatedAt,desc"),
    IMAGE_COUNT_DESC(label = "사진 많은 순", queryValue = "imageCount,desc"),
}