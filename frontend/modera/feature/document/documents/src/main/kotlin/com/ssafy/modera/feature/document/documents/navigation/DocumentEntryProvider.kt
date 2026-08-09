package com.ssafy.modera.feature.document.documents.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.feature.document.documents.DocumentScreen
import com.ssafy.modera.feature.document.documents.DocumentViewModel

fun EntryProviderScope<NavKey>.documentEntry(
    onDocumentClick: (Long) -> Unit,
) {
    entry<DocumentNavKey> {
        val viewModel = hiltViewModel<DocumentViewModel>()

        DocumentScreen(
            viewModel = viewModel,
            onDocumentClick = onDocumentClick,
        )
    }
}