package com.ssafy.modera.feature.document.documentcreate.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.document.documentcreate.DocumentCreateMode
import com.ssafy.modera.feature.document.documentcreate.DocumentCreateScreen
import com.ssafy.modera.feature.document.documentcreate.DocumentCreateViewModel
import com.ssafy.modera.feature.document.documentcreate.DocumentCreateViewModel.Factory

fun EntryProviderScope<NavKey>.documentCreateEntry(
    navigator: Navigator,
    onBackClick: () -> Unit,
    onDocumentCreated: (Long) -> Unit,
) {
    entry<DocumentCreateNavKey> { key ->
        DocumentCreateDestination(
            viewModelKey = "document-create-${key.image.id}",
            mode = DocumentCreateMode.Create(
                initialImage = key.image.asAnalyzedImage(),
            ),
            onBackClick = onBackClick,
            onDocumentCreated = onDocumentCreated,
        )
    }

    entry<DocumentRecreateNavKey> { key ->
        DocumentCreateDestination(
            viewModelKey = "document-recreate-${key.documentId}",
            mode = DocumentCreateMode.Recreate(
                documentId = key.documentId,
                initialImages = key.images.map { image ->
                    image.asAnalyzedImage()
                },
            ),
            onBackClick = onBackClick,
            onDocumentCreated = onDocumentCreated,
        )
    }
}

@Composable
private fun DocumentCreateDestination(
    viewModelKey: String,
    mode: DocumentCreateMode,
    onBackClick: () -> Unit,
    onDocumentCreated: (Long) -> Unit,
) {
    val viewModel =
        hiltViewModel<DocumentCreateViewModel, Factory>(
            key = viewModelKey,
        ) { factory ->
            factory.create(
                mode = mode,
            )
        }

    DocumentCreateScreen(
        viewModel = viewModel,
        onBackClick = onBackClick,
        onDocumentCreated = onDocumentCreated,
    )
}