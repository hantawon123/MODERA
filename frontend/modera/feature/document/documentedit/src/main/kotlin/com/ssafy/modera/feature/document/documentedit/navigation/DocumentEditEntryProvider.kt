package com.ssafy.modera.feature.document.documentedit.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.feature.document.documentedit.DocumentEditScreen
import com.ssafy.modera.feature.document.documentedit.DocumentEditViewModel
import com.ssafy.modera.feature.document.documentedit.DocumentEditViewModel.Factory

fun EntryProviderScope<NavKey>.documentEditEntry(
    onBackClick: () -> Unit,
    onEditClick: (List<AnalyzedImage>) -> Unit,
    onImageClick: (Long) -> Unit,
) {
    entry<DocumentEditNavKey> { key ->
        val documentId = key.documentId

        val viewModel =
            hiltViewModel<DocumentEditViewModel, Factory>(
                key = "document-edit-$documentId",
            ) { factory ->
                factory.create(documentId)
            }

        DocumentEditScreen(
            viewModel = viewModel,
            onBackClick = onBackClick,
            onEditClick = onEditClick,
            onImageClick = onImageClick,
        )
    }
}