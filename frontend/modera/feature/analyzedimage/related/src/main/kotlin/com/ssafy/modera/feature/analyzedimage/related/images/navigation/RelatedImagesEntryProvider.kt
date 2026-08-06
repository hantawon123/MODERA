package com.ssafy.modera.feature.analyzedimage.related.images.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.analyzedimage.api.navigation.RelatedImagesNavKey
import com.ssafy.modera.feature.analyzedimage.api.navigation.navigateToImageDetail
import com.ssafy.modera.feature.analyzedimage.related.images.RelatedImagesScreen
import com.ssafy.modera.feature.analyzedimage.related.images.RelatedImagesViewModel
import com.ssafy.modera.feature.analyzedimage.related.images.RelatedImagesViewModel.Factory

fun EntryProviderScope<NavKey>.relatedImagesEntry(navigator: Navigator) {
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
            onBackClick = navigator::popBackStack,
            onRelatedImageClick = navigator::navigateToImageDetail,
        )
    }
}