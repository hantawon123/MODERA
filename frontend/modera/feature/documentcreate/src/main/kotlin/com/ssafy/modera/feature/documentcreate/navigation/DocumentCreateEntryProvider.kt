package com.ssafy.modera.feature.documentcreate.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.documentcreate.DocumentCreateScreen
import com.ssafy.modera.feature.documentcreate.DocumentCreateViewModel
import com.ssafy.modera.feature.documentcreate.DocumentCreateViewModel.Factory

fun EntryProviderScope<NavKey>.documentCreateEntry(
    navigator: Navigator,
    onBackClick: () -> Unit,
    onDocumentCreated: (Long) -> Unit,
) {
    entry<DocumentCreateNavKey> { key ->
        val viewModel =
            hiltViewModel<DocumentCreateViewModel, Factory>(
                key = "document-create-${key.imageId}",
            ) { factory ->
                factory.create(
                    initialImage = key.asInitialImage(),
                )
            }

        DocumentCreateScreen(
            viewModel = viewModel,
            onBackClick = onBackClick,
            onDocumentCreated = onDocumentCreated
        )
    }
}