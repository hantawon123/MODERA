package com.ssafy.modera.feature.relatedimages.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.analyzedimagedetail.navigation.navigateToImageDetail
import com.ssafy.modera.feature.relatedimages.RelatedImagesScreen
import com.ssafy.modera.feature.relatedimages.RelatedImagesViewModel
import com.ssafy.modera.feature.relatedimages.RelatedImagesViewModel.Factory

fun EntryProviderScope<NavKey>.relatedImagesEntry(
    navigator: Navigator,
    onBackClick: () -> Unit,
) {
    entry<RelatedImagesNavKey> { key ->
        val viewModel =
            hiltViewModel<RelatedImagesViewModel, Factory>(
                key = "related-images-${key.imageId}",
            ) { factory ->
                factory.create(
                    imageId = key.imageId,
                )
            }

        RelatedImagesScreen(
            sourceTitle = key.sourceTitle,
            viewModel = viewModel,
            onBackClick = onBackClick,
            onRelatedImageClick = { imageId ->
                navigator.navigateToImageDetail(
                    imageId = imageId,
                )
            },
        )
    }
}