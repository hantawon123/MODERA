package com.ssafy.modera.feature.home

import com.ssafy.modera.core.model.category.CategorySortType

val CategorySortType.label: String
    get() = when (this) {
        CategorySortType.NAME_ASC -> "이름순"
        CategorySortType.UPDATED_AT_DESC -> "최신 업로드순"
        CategorySortType.IMAGE_COUNT_DESC -> "사진 많은 순"
    }