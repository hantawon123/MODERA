package com.ssafy.modera.feature.document.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.document.Document
import com.ssafy.modera.core.model.document.DocumentSortType
import com.ssafy.modera.core.ui.ErrorScreen
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.document.DocumentScreenPreviewData
import com.ssafy.modera.feature.document.DocumentScreenPreviewParameterProvider
import com.ssafy.modera.feature.document.R
import com.ssafy.modera.feature.document.documents.component.DocumentEmptyScreen
import com.ssafy.modera.feature.document.documents.component.DocumentItem
import com.ssafy.modera.feature.document.documents.component.DocumentListHeader
import com.ssafy.modera.feature.document.documents.component.DocumentTopBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
internal fun DocumentScreen(
    viewModel: DocumentViewModel,
    onDocumentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(
        event = Lifecycle.Event.ON_RESUME,
    ) {
        viewModel.onScreenResumed()
    }

    DocumentScreen(
        uiState = uiState,
        onDocumentClick = onDocumentClick,
        onSortTypeChange = viewModel::updateSortType,
        onLoadMore = viewModel::loadNextPage,
        modifier = modifier,
    )
}

@Composable
private fun DocumentScreen(
    uiState: DocumentUiState,
    onDocumentClick: (Long) -> Unit,
    onSortTypeChange: (DocumentSortType) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white),
    ) {
        DocumentTopBar()

        when (uiState) {
            DocumentUiState.Loading -> {
                LoadingScreen(
                    modifier = Modifier.weight(1f),
                )
            }

            is DocumentUiState.Success -> {
                DocumentContent(
                    documents = uiState.documents,
                    sortType = uiState.sortType,
                    onDocumentClick = onDocumentClick,
                    onSortTypeChange = onSortTypeChange,
                    onLoadMore = onLoadMore,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                )
            }

            DocumentUiState.Empty -> {
                DocumentEmptyScreen()
            }

            is DocumentUiState.Error -> {
                ErrorScreen(
                    message = stringResource(
                        R.string.document_load_error,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DocumentContent(
    documents: List<Document>,
    sortType: DocumentSortType,
    onDocumentClick: (Long) -> Unit,
    onSortTypeChange: (DocumentSortType) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(
        listState,
        documents.size,
    ) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex =
                layoutInfo.visibleItemsInfo.lastOrNull()?.index

            totalItemCount > 0 &&
                    lastVisibleItemIndex != null &&
                    lastVisibleItemIndex >=
                    totalItemCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { shouldLoadMore -> shouldLoadMore }
            .collect {
                onLoadMore()
            }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        DocumentListHeader(
            documentCount = documents.size,
            sortType = sortType,
            onSortTypeChange = onSortTypeChange,
        )

        HorizontalDivider(
            color = ModeraTheme.colors.gray200,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(
                items = documents,
                key = Document::id,
            ) { document ->
                DocumentItem(
                    document = document,
                    onClick = {
                        onDocumentClick(document.id)
                    },
                )

                HorizontalDivider(
                    color = ModeraTheme.colors.gray200,
                )
            }
        }
    }
}

@Preview(
    name = "Document Screen",
    showBackground = true,
)
@Composable
private fun DocumentScreenPreview(
    @PreviewParameter(DocumentScreenPreviewParameterProvider::class)
    previewData: DocumentScreenPreviewData,
) {
    ModeraTheme {
        DocumentScreen(
            uiState = previewData.uiState,
            onDocumentClick = {},
            onSortTypeChange = {},
            onLoadMore = {},
        )
    }
}

private const val LOAD_MORE_THRESHOLD = 3