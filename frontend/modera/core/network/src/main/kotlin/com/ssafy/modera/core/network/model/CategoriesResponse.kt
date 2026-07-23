package com.ssafy.modera.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoriesResponse(
    val list: List<CategoryResponse>,
)