package com.ssafy.modera.feature.analyzedimage.related.documents.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.analyzedimage.api.navigation.RelatedDocumentsNavKey
import com.ssafy.modera.feature.analyzedimage.api.navigation.navigateToImageDetail
import com.ssafy.modera.feature.analyzedimage.related.documents.RelatedDocumentsScreen
import com.ssafy.modera.feature.analyzedimage.related.documents.RelatedDocumentsViewModel
import com.ssafy.modera.feature.analyzedimage.related.documents.RelatedDocumentsViewModel.Factory

fun EntryProviderScope<NavKey>.relatedDocumentsEntry(navigator: Navigator) {
    entry<RelatedDocumentsNavKey> { key ->
        val viewModel =
            hiltViewModel<RelatedDocumentsViewModel, Factory>(
                key = "related-documents-${key.imageId}",
            ) { factory ->
                factory.create(
                    imageId = key.imageId,
                )
            }

        RelatedDocumentsScreen(
            sourceTitle = key.sourceTitle,
            viewModel = viewModel,
            onBackClick = navigator::popBackStack,
            onDocumentClick = navigator::navigateToImageDetail,
        )
    }
}