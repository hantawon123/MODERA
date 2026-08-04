package com.ssafy.modera.feature.document.documentdetail.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class DocumentDetailNavKey(
    val documentId: Long,
) : NavKey