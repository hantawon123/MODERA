package com.ssafy.modera.feature.document.documentcreate.navigation

import kotlinx.serialization.Serializable

@Serializable
data class DocumentCreateImageArg(
    val id: Long,
    val title: String,
    val summary: String,
    val thumbnailUrl: String,
    val hashtags: List<String>,
    val favorite: Boolean,
)