package com.ssafy.modera.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.analyzedimage.detail.navigation.analyzedImageDetailEntry
import com.ssafy.modera.feature.analyzedimage.related.documents.navigation.relatedDocumentsEntry
import com.ssafy.modera.feature.analyzedimage.related.images.navigation.relatedImagesEntry
import com.ssafy.modera.feature.calendar.navigation.navigateToCalendar
import com.ssafy.modera.feature.document.documentcreate.navigation.navigateToDocumentCreate
import com.ssafy.modera.feature.imageviewer.navigation.navigateToImageViewer

internal fun EntryProviderScope<NavKey>.analyzedImageEntries(
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope,
) {
    analyzedImageDetailEntry(
        navigator = navigator,
        sharedTransitionScope = sharedTransitionScope,
        onImageClick = navigator::navigateToImageViewer,
        onCreateDocumentClick = navigator::navigateToDocumentCreate,
        onNavigateToCalendar = navigator::navigateToCalendar,
    )

    relatedImagesEntry(navigator)
    relatedDocumentsEntry(navigator)
}