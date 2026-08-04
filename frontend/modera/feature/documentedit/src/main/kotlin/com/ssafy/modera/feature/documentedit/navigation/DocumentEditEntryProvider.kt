package com.ssafy.modera.feature.documentedit.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.feature.documentedit.DocumentEditScreen
import com.ssafy.modera.feature.documentedit.DocumentEditViewModel
import com.ssafy.modera.feature.documentedit.DocumentEditViewModel.Factory

fun EntryProviderScope<NavKey>.documentEditEntry(
    onBackClick: () -> Unit,
    onDocumentCreated: (Long) -> Unit,
    onAddImagesClick: () -> Unit,
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
            onDocumentCreated = onDocumentCreated,
            onAddImagesClick = onAddImagesClick,
        )
    }
}