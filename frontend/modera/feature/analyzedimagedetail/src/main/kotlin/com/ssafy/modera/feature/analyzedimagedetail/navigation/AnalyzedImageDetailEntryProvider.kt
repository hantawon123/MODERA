package com.ssafy.modera.feature.analyzedimagedetail.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.analyzedimagedetail.AnalyzedImageDetailScreen
import com.ssafy.modera.feature.analyzedimagedetail.AnalyzedImageDetailViewModel
import com.ssafy.modera.feature.analyzedimagedetail.AnalyzedImageDetailViewModel.Factory
import com.ssafy.modera.feature.relatedimages.navigation.navigateToRelatedImages

fun EntryProviderScope<NavKey>.analyzedImageDetailEntry(
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope,
    onImageClick:(String) -> Unit,
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
            onBackClick = navigator::goBack,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            onImageClick = onImageClick,
            onFavoriteClick = {
                // TODO ViewModel 즐겨찾기 기능 연결
            },
            onDocumentClick = {
                // TODO 문서화 화면 이동
            },
            onScheduleClick = {
                // TODO 일정 화면 이동
            },
            onReanalyzeClick = {
                // TODO ViewModel 재분석 기능 연결
            },
            onDeleteClick = {
                // TODO ViewModel 삭제 기능 연결
            },
            onRelatedImagesClick = { imageId, sourceTitle ->
                navigator.navigateToRelatedImages(imageId, sourceTitle)
            },
        )
    }
}