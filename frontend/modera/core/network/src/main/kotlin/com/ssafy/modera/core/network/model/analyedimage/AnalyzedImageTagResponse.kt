package com.ssafy.modera.core.network.model.analyedimage

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedImageTagResponse(
    val tagId: Long,
    val name: String,
)
