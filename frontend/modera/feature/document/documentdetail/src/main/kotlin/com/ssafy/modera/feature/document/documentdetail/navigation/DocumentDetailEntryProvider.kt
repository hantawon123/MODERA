package com.ssafy.modera.feature.document.documentdetail.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.feature.document.documentdetail.DocumentDetailScreen
import com.ssafy.modera.feature.document.documentdetail.DocumentDetailViewModel
import com.ssafy.modera.feature.document.documentdetail.DocumentDetailViewModel.Factory

fun EntryProviderScope<NavKey>.documentDetailEntry(
    onBackClick: () -> Unit,
    onManageImagesClick: (Long) -> Unit,
) {
    entry<DocumentDetailNavKey> { key ->
        val viewModel =
            hiltViewModel<DocumentDetailViewModel, Factory>(
                key = "document-detail-${key.documentId}",
            ) { factory ->
                factory.create(
                    documentId = key.documentId,
                )
            }

        DocumentDetailScreen(
            viewModel = viewModel,
            onBackClick = onBackClick,
            onManageImagesClick = onManageImagesClick,
        )
    }
}