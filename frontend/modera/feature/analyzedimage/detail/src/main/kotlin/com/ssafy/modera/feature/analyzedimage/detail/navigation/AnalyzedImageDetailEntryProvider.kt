package com.ssafy.modera.feature.analyzedimage.detail.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.analyzedimage.api.navigation.AnalyzedImageDetailNavKey
import com.ssafy.modera.feature.analyzedimage.api.navigation.navigateToRelatedDocuments
import com.ssafy.modera.feature.analyzedimage.api.navigation.navigateToRelatedImages
import com.ssafy.modera.feature.analyzedimage.detail.AnalyzedImageDetailScreen
import com.ssafy.modera.feature.analyzedimage.detail.AnalyzedImageDetailViewModel
import com.ssafy.modera.feature.analyzedimage.detail.AnalyzedImageDetailViewModel.Factory

fun EntryProviderScope<NavKey>.analyzedImageDetailEntry(
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope,
    onImageClick: (String) -> Unit,
    onCreateDocumentClick: (AnalyzedImage) -> Unit,
) {
    entry<AnalyzedImageDetailNavKey> { key ->
        val imageId = key.imageId

        val viewModel =
            hiltViewModel<AnalyzedImageDetailViewModel, Factory>(
                key = "analyzed-image-detail-$imageId",
            ) { factory ->
                factory.create(imageId)
            }

        AnalyzedImageDetailScreen(
            viewModel = viewModel,
            onBackClick = navigator::popBackStack,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            onImageClick = onImageClick,
            onCreateDocumentClick = onCreateDocumentClick,
            onRelatedDocumentClick = navigator::navigateToRelatedDocuments,
            onRelatedScheduleClick = {
                // TODO 일정 화면 이동
            },
            onRelatedImagesClick = navigator::navigateToRelatedImages,
        )
    }
}