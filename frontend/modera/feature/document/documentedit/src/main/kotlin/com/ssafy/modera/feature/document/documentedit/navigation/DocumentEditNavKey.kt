package com.ssafy.modera.feature.document.documentedit.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class DocumentEditNavKey(
    val documentId: Long,
) : NavKey

fun Navigator.navigateToDocumentEdit(
    documentId: Long,
) {
    navigate(
        DocumentEditNavKey(
            documentId = documentId,
        ),
    )
}