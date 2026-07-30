package com.ssafy.modera.feature.imageviewer.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.ssafy.modera.feature.imageviewer.ImageViewerScreen

@OptIn(ExperimentalSharedTransitionApi::class)
fun EntryProviderScope<NavKey>.imageViewerEntry(
    sharedTransitionScope: SharedTransitionScope,
    onBackClick: () -> Unit,
) {
    entry<ImageViewerNavKey> { key ->
        ImageViewerScreen(
            imageUrl = key.imageUrl,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            onBackClick = onBackClick,
        )
    }
}