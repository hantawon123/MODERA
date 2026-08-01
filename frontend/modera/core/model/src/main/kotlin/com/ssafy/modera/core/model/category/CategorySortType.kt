package com.ssafy.modera.core.model.category

enum class CategorySortType(
    val label: String,
    val queryValue: String,
) {
    NAME_ASC(label = "이름순", queryValue = "NAME_ASC"),
    UPDATED_DESC(label = "최신 순", queryValue = "UPDATED_DESC"),
    IMAGE_COUNT_DESC(label = "사진 많은 순", queryValue = "IMAGE_COUNT_DESC"),
}
