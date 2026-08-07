package com.ssafy.modera.feature.analyzedimage.related.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.component.item.ModeraDocumentItem
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.document.Document
import com.ssafy.modera.core.ui.EmptyScreen
import com.ssafy.modera.core.ui.ErrorScreen
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.analyzedimage.related.R
import com.ssafy.modera.feature.analyzedimage.related.component.RelatedHeader

@Composable
fun RelatedDocumentsScreen(
    sourceTitle: String,
    onBackClick: () -> Unit,
    onDocumentClick: (Long) -> Unit,
    viewModel: RelatedDocumentsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RelatedDocumentsScreen(
        sourceTitle = sourceTitle,
        uiState = uiState,
        onBackClick = onBackClick,
        onDocumentClick = onDocumentClick,
        modifier = modifier,
    )
}

@Composable
fun RelatedDocumentsScreen(
    sourceTitle: String,
    uiState: RelatedDocumentsUiState,
    onBackClick: () -> Unit,
    onDocumentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white),
    ) {
        ModeraTopBar(
            onBackClick = onBackClick,
            centerContent = {
                Text(
                    text = stringResource(R.string.related_documents_title),
                    style = ModeraTheme.typography.bodySB16,
                    color = ModeraTheme.colors.gray900,
                    maxLines = 1,
                )
            },
        )

        when (uiState) {
            RelatedDocumentsUiState.Loading -> {
                LoadingScreen(modifier = Modifier.weight(1f))
            }

            is RelatedDocumentsUiState.Success -> {
                RelatedDocumentsContent(
                    sourceTitle = sourceTitle,
                    relatedDocuments = uiState.relatedDocuments,
                    onDocumentClick = onDocumentClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                )
            }

            RelatedDocumentsUiState.Empty -> {
                EmptyScreen(
                    message = "연관 문서가 없습니다.",
                    modifier = Modifier.weight(1f),
                )
            }

            is RelatedDocumentsUiState.Error -> {
                ErrorScreen(
                    message = "연관 문서를 불러오지 못했습니다.",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RelatedDocumentsContent(
    sourceTitle: String,
    relatedDocuments: List<Document>,
    onDocumentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item(key = "related_documents_header") {
            RelatedHeader(
                sourceTitle = sourceTitle,
                relatedImageCount = relatedDocuments.size,
                headerSuffix = stringResource(R.string.related_documents_header_suffix),
                modifier = Modifier.padding(vertical = 20.dp),
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = ModeraTheme.colors.gray200,
            )
        }

        items(
            items = relatedDocuments,
            key = Document::id,
        ) { document ->
            ModeraDocumentItem(
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

@Preview(
    name = "Related Documents",
    showBackground = true,
)
@Composable
private fun RelatedDocumentsScreenPreview(
    @PreviewParameter(RelatedDocumentsScreenPreviewParameterProvider::class)
    previewData: RelatedDocumentsScreenPreviewData,
) {
    ModeraTheme {
        RelatedDocumentsScreen(
            sourceTitle = previewData.sourceTitle,
            uiState = previewData.uiState,
            onBackClick = {},
            onDocumentClick = {},
        )
    }
}