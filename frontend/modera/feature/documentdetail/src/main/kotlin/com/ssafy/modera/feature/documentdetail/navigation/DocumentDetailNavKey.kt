package com.ssafy.modera.feature.documentdetail.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class DocumentDetailNavKey(
    val documentId: Long,
) : NavKey

fun Navigator.navigateToDocumentDetail(
    documentId: Long,
) {
    navigate(
        DocumentDetailNavKey(
            documentId = documentId,
        ),
    )
}