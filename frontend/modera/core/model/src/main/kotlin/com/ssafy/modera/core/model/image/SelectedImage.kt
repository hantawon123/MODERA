package com.ssafy.modera.core.model.image

data class SelectedImage(
    val uri: String,
    val originalFileName: String,
    val fileSizeBytes: Long,
    val ocrText: String = "",
)
