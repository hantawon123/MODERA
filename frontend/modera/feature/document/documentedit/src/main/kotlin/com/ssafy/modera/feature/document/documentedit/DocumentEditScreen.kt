package com.ssafy.modera.feature.document.documentedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.component.item.ModeraMaterialCard
import com.ssafy.modera.core.component.item.ModeraMaterialItem
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.ui.ErrorScreen
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.document.documentedit.component.DocumentEditTopBar
import com.ssafy.modera.feature.documentedit.R

@Composable
internal fun DocumentEditScreen(
    viewModel: DocumentEditViewModel,
    onBackClick: () -> Unit,
    onEditClick: (List<AnalyzedImage>) -> Unit,
    onImageClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DocumentEditScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onEditClick = onEditClick,
        onImageClick = onImageClick,
        modifier = modifier,
    )
}

@Composable
private fun DocumentEditScreen(
    uiState: DocumentEditUiState,
    onBackClick: () -> Unit,
    onEditClick: (List<AnalyzedImage>) -> Unit,
    onImageClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white),
    ) {
        DocumentEditTopBar(
            onBackClick = onBackClick,
            onEditClick = {
                if (uiState is DocumentEditUiState.Success) {
                    onEditClick(uiState.images)
                }
            },
        )

        when (uiState) {
            DocumentEditUiState.Loading -> {
                LoadingScreen(
                    modifier = Modifier.weight(1f),
                )
            }

            is DocumentEditUiState.Success -> {
                DocumentEditContent(
                    state = uiState,
                    onImageClick = onImageClick,
                    modifier = Modifier.weight(1f),
                )
            }

            is DocumentEditUiState.Error -> {
                ErrorScreen(
                    message = stringResource(
                        R.string.document_edit_load_error,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DocumentEditContent(
    state: DocumentEditUiState.Success,
    onImageClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = 24.dp,
            vertical = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = state.images,
            key = AnalyzedImage::id,
        ) { image ->
            ModeraMaterialCard(
                title = image.title,
                description = image.summary,
                tags = image.hashtags.take(3),
                imageUrl = image.thumbnailUrl,
                onClick = {
                    onImageClick(image.id)
                },
            )
        }
    }
}

@Preview(name = "DocumentEditScreen", showBackground = true)
@Composable
private fun DocumentEditScreenPreview(
    @PreviewParameter(
        DocumentEditScreenPreviewParameterProvider::class,
    )
    previewData: DocumentEditScreenPreviewData,
) {
    ModeraTheme {
        DocumentEditScreen(
            uiState = previewData.uiState,
            onBackClick = {},
            onEditClick = {},
            onImageClick = {},
        )
    }
}