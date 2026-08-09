package com.ssafy.modera.core.model.category

data class CategorySheetItem(
    val id: Long,
    val title: String,
    val itemCount: Int,
    val isNew: Boolean = false,
    val isAll: Boolean = false,
) {
    companion object {
        const val ALL_CATEGORY_ID = 0L
    }
}