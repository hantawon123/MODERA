package com.ssafy.modera.feature.documentdetail.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.feature.documentdetail.DocumentDetailScreen
import com.ssafy.modera.feature.documentdetail.DocumentDetailViewModel
import com.ssafy.modera.feature.documentdetail.DocumentDetailViewModel.Factory

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