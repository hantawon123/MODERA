package com.ssafy.modera.core.model.category

data class Category(
    val id: Long,
    val title: String,
    val thumbnailUrl: String?,
    val itemCount: Int,
    val tags: List<String>,
    val isNew: Boolean = false,
)