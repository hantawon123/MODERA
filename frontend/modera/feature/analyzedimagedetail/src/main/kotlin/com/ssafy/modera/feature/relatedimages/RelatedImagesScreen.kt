package com.ssafy.modera.feature.relatedimages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.ssafy.modera.core.component.item.ModeraMaterialItem
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.ui.ErrorScreen
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.analyzedimagedetail.R
import com.ssafy.modera.feature.relatedimages.component.RelatedImagesHeader

@Composable
fun RelatedImagesScreen(
    sourceTitle: String,
    onBackClick: () -> Unit,
    onRelatedImageClick: (Long) -> Unit,
    viewModel: RelatedImagesViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RelatedImagesScreen(
        sourceTitle = sourceTitle,
        uiState = uiState,
        onBackClick = onBackClick,
        onRelatedImageClick = onRelatedImageClick,
        modifier = modifier,
    )
}

@Composable
fun RelatedImagesScreen(
    sourceTitle: String,
    uiState: RelatedImagesUiState,
    onBackClick: () -> Unit,
    onRelatedImageClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Vertical,
                ),
            ),
    ) {
        ModeraTopBar(
            onBackClick = onBackClick,
            centerContent = {
                Text(
                    text = stringResource(R.string.related_images_title),
                    style = ModeraTheme.typography.bodySB16,
                    color = ModeraTheme.colors.gray900,
                    maxLines = 1,
                )
            },
        )

        when (uiState) {
            RelatedImagesUiState.Loading -> {
                LoadingScreen(modifier = Modifier.weight(1f))
            }

            is RelatedImagesUiState.Success -> {
                RelatedImagesContent(
                    sourceTitle = sourceTitle,
                    relatedImages = uiState.relatedImages,
                    onRelatedImageClick = onRelatedImageClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                )
            }

            RelatedImagesUiState.Empty -> {
                ErrorScreen(
                    message = "연관 자료가 없습니다.",
                    modifier = Modifier.weight(1f),
                )
            }

            is RelatedImagesUiState.Error -> {
                ErrorScreen(
                    message = uiState.message,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RelatedImagesContent(
    sourceTitle: String,
    relatedImages: List<AnalyzedImage>,
    onRelatedImageClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item(key = "related_images_header") {
            RelatedImagesHeader(
                sourceTitle = sourceTitle,
                relatedImageCount = relatedImages.size,
                modifier = Modifier.padding(vertical = 20.dp),
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = ModeraTheme.colors.gray200,
            )
        }

        items(
            items = relatedImages,
            key = AnalyzedImage::id,
        ) { image ->
            ModeraMaterialItem(
                title = image.title,
                description = image.summary,
                tags = image.hashtags,
                imageUrl = image.thumbnailUrl,
                onClick = {
                    onRelatedImageClick(image.id)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Related Images Screen", showBackground = true)
@Composable
private fun RelatedImagesScreenPreview(
    @PreviewParameter(RelatedImagesScreenPreviewParameterProvider::class)
    previewData: RelatedImagesScreenPreviewData,
) {
    ModeraTheme {
        RelatedImagesScreen(
            sourceTitle = previewData.sourceTitle,
            uiState = previewData.uiState,
            onBackClick = {},
            onRelatedImageClick = {},
        )
    }
}