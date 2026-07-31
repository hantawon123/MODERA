package com.ssafy.modera.feature.documentcreate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraIconTextButton
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.component.item.ModeraMaterialItem
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.IconButton
import com.ssafy.modera.core.designsystem.component.ModeraIconButtonDefaults
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import com.ssafy.modera.core.ui.ErrorScreen
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.documentcreate.component.SelectedImagesSection

@Composable
internal fun DocumentCreateScreen(
    uiState: DocumentCreateUiState,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSelectedImageRemoveClick: (Long) -> Unit,
    onRecommendedImageClick: (Long) -> Unit,
    onCreateDocumentClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isRefreshEnabled by remember {
        mutableStateOf(false)
    }

    val recommendedListState = rememberLazyListState()

    LaunchedEffect(isRefreshEnabled) {
        if (!isRefreshEnabled) {
            recommendedListState.scrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white),
    ) {
        ModeraTopBar(
            onBackClick = onBackClick,
            modifier = modifier,
            centerContent = {
                Text(
                    text = stringResource(
                        R.string.document_create_title,
                    ),
                    style = ModeraTheme.typography.bodySB16,
                    color = ModeraTheme.colors.gray900,
                    maxLines = 1,
                )
            },
            rightContent = {
                IconButton(
                    imageVector = ImageVector.vectorResource(ModeraIcons.Refresh),
                    onClick = {
                        isRefreshEnabled = false
                        onRefreshClick()
                    },
                    size = 24.dp,
                    enabled = isRefreshEnabled,
                    colors = ModeraIconButtonDefaults.iconButtonColors(
                        contentColor = ModeraTheme.colors.gray700,
                        disabledContentColor = ModeraTheme.colors.gray200,
                    )
                )
            }
        )

        when (uiState) {
            DocumentCreateUiState.Loading -> {
                LoadingScreen(
                    modifier = Modifier.weight(1f),
                )
            }

            is DocumentCreateUiState.Success -> {
                DocumentCreateContent(
                    uiState = uiState,
                    recommendedListState = recommendedListState,
                    onSelectedImageRemoveClick = { imageId ->
                        isRefreshEnabled = true
                        onSelectedImageRemoveClick(imageId)
                    },
                    onRecommendedImageClick = { imageId ->
                        isRefreshEnabled = true
                        onRecommendedImageClick(imageId)
                    },
                    onCreateDocumentClick =
                        onCreateDocumentClick,
                    modifier = Modifier.weight(1f),
                )
            }

            is DocumentCreateUiState.Error -> {
                ErrorScreen(
                    message = stringResource(
                        R.string.document_create_load_error,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DocumentCreateContent(
    uiState: DocumentCreateUiState.Success,
    recommendedListState: LazyListState,
    onSelectedImageRemoveClick: (Long) -> Unit,
    onRecommendedImageClick: (Long) -> Unit,
    onCreateDocumentClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    R.string.document_create_recommendation_guide,
                ),
                style = ModeraTheme.typography.captionSB12,
                color = ModeraTheme.colors.yellow700,
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = ModeraTheme.colors.gray200,
            )
        }
        LazyColumn(
            state = recommendedListState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 24.dp),
        ) {
            if (uiState.isRecommendationLoading) {
                item(
                    key = "recommendation_loading",
                ) {
                    LoadingScreen(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }
            } else {
                items(
                    items = uiState.recommendedImages,
                    key = AnalyzedImage::id,
                ) { image ->
                    ModeraMaterialItem(
                        title = image.title,
                        description = image.summary,
                        tags = image.hashtags,
                        imageUrl = image.thumbnailUrl,
                        onClick = {
                            onRecommendedImageClick(image.id)
                        },
                    )
                }
            }
        }

        val shape = RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = shape,
                    clip = false,
                )
                .clip(shape)
                .background(ModeraTheme.colors.white)
        ) {
            SelectedImagesSection(
                images = uiState.selectedImages,
                onRemoveClick = onSelectedImageRemoveClick,
            )

            ModeraIconTextButton(
                text = stringResource(
                    R.string.document_create_button_label,
                ),
                icon = painterResource(
                    ModeraIcons.FileAdd,
                ),
                enabled = uiState.canCreateDocument,
                onClick = onCreateDocumentClick,
                buttonColor = if (uiState.canCreateDocument) {
                    ModeraTheme.colors.yellow700
                } else {
                    ModeraTheme.colors.white
                },
                contentColor = if (uiState.canCreateDocument) {
                    ModeraTheme.colors.white
                } else {
                    ModeraTheme.colors.gray500
                },
                borderColor = if (uiState.canCreateDocument) {
                    ModeraTheme.colors.white
                } else {
                    ModeraTheme.colors.gray400
                },
                iconContentDescription = stringResource(
                    R.string.document_create_button_icon_description,
                ),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 16.dp,
                    ),
            )
        }
    }
}

@Preview(name = "DocumentCreateScreen", showBackground = true)
@Composable
private fun DocumentCreateScreenPreview() {
    var uiState by remember {
        mutableStateOf(
            DocumentCreateUiState.Success(
                selectedImages =
                    previewAnalyzedImages.take(5),
                recommendedImages =
                    previewAnalyzedImages.drop(5),
            ),
        )
    }

    ModeraTheme {
        DocumentCreateScreen(
            uiState = uiState,
            onBackClick = {},
            onSelectedImageRemoveClick = { imageId ->
                uiState = uiState.copy(
                    selectedImages =
                        uiState.selectedImages.filterNot {
                            it.id == imageId
                        },
                )
            },
            onRecommendedImageClick = { imageId ->
                val selectedImage =
                    uiState.recommendedImages
                        .firstOrNull {
                            it.id == imageId
                        }
                        ?: return@DocumentCreateScreen

                uiState = uiState.copy(
                    selectedImages =
                        uiState.selectedImages + selectedImage,
                    recommendedImages =
                        uiState.recommendedImages.filterNot {
                            it.id == imageId
                        },
                )
            },
            onCreateDocumentClick = {},
            onRefreshClick = {
                uiState = DocumentCreateUiState.Success(
                    selectedImages =
                        previewAnalyzedImages.take(5),
                    recommendedImages =
                        previewAnalyzedImages.drop(5),
                )
            },

            )
    }
}

private val previewAnalyzedImages = List(20) { index ->
    AnalyzedImage(
        id = index.toLong(),
        title = "성심당 케이크 리스트",
        summary = "올해 성심당 케이크 메뉴 리스트로, "
                + "샤인머스켓 시루, 귤 시루, 맛있겠다.",
        thumbnailUrl =
            "https://picsum.photos/seed/"
                    + "document-create-$index/300/300",
        hashtags = listOf(
            "기차",
            "예약",
            "KTX",
        ),
        status = ImageAnalysisStatus.COMPLETED,
        favorite = false,
    )
}