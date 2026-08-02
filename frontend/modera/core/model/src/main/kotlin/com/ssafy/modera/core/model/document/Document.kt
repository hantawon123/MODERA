package com.ssafy.modera.core.model.document

data class Document(
    val id: Long,
    val title: String,
    val content: String,
    val sourceImageCount: Int,
    val updatedAt: Long,
)