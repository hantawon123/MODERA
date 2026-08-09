package com.ssafy.modera.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val result: String,
    val code: String,
    val message: String,
    val data: T,
    val timestamp: String? = null,
)
