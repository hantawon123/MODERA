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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.ssafy.modera.core.ui.DocumentCreatingScreen
import com.ssafy.modera.core.ui.ErrorScreen
import com.ssafy.modera.feature.documentcreate.component.SelectedImagesSection

@Composable
internal fun DocumentCreateScreen(
    viewModel: DocumentCreateViewModel,
    onDocumentCreated: (Long) -> Unit,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    val hasSelectionChanged by viewModel.hasSelectionChanged.collectAsStateWithLifecycle()

    DocumentCreateScreen(
        uiState = uiState,
        selectedImages = selectedImages,
        hasSelectionChanged = hasSelectionChanged,
        onBackClick = onBackClick,
        onRefreshClick = viewModel::refreshRecommendedImages,
        onSelectedImageRemoveClick = viewModel::removeSelectedImage,
        onRecommendedImageClick = viewModel::addSelectedImage,
        onCreateDocumentClick = {
            viewModel.createDocument { documentId ->
                onDocumentCreated(documentId)
            }
        },
    )
}

@Composable
private fun DocumentCreateScreen(
    uiState: DocumentCreateUiState,
    selectedImages: List<AnalyzedImage>,
    hasSelectionChanged: Boolean,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSelectedImageRemoveClick: (Long) -> Unit,
    onRecommendedImageClick: (AnalyzedImage) -> Unit,
    onCreateDocumentClick: () -> Unit,
    modifier: Modifier = Modifier,
) {


    val recommendedListState = rememberLazyListState()

    val canCreateDocument =
        selectedImages.size > 1 &&
                uiState is DocumentCreateUiState.Success
    val canRefresh =
        hasSelectionChanged &&
                uiState is DocumentCreateUiState.Success

    LaunchedEffect(hasSelectionChanged) {
        if (!hasSelectionChanged) {
            recommendedListState.scrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white),
    ) {
        val isCreating = uiState is DocumentCreateUiState.Creating
        ModeraTopBar(
            onBackClick = onBackClick,
            centerContent = {
                if (!isCreating) {
                    Text(
                        text = stringResource(
                            R.string.document_create_title,
                        ),
                        style = ModeraTheme.typography.bodySB16,
                        color = ModeraTheme.colors.gray900,
                        maxLines = 1,
                    )
                }
            },
            rightContent = {
                if (!isCreating) {
                    IconButton(
                        imageVector = ImageVector.vectorResource(
                            ModeraIcons.Refresh,
                        ),
                        onClick = onRefreshClick,
                        size = 24.dp,
                        enabled = canRefresh,
                        colors = ModeraIconButtonDefaults.iconButtonColors(
                            contentColor = ModeraTheme.colors.gray700,
                            disabledContentColor = ModeraTheme.colors.gray200,
                        ),
                    )
                }
            },
        )

        when (uiState) {
            DocumentCreateUiState.Loading,
            is DocumentCreateUiState.Success,
                -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.document_create_recommendation_guide,
                            ),
                            style = ModeraTheme.typography.captionSB12,
                            color = ModeraTheme.colors.yellow700,
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp),
                        )

                        HorizontalDivider(
                            color = ModeraTheme.colors.gray200,
                        )
                    }

                    LazyColumn(
                        state = recommendedListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                    ) {
                        when (uiState) {
                            DocumentCreateUiState.Loading -> {
                                item(
                                    key = "recommendation_loading",
                                ) {
                                    DocumentRecommendationSkeleton()
                                }
                            }

                            is DocumentCreateUiState.Success -> {
                                items(
                                    items = uiState.recommendedImages,
                                    key = AnalyzedImage::id,
                                ) { image ->
                                    ModeraMaterialItem(
                                        title = image.title,
                                        description = image.summary,
                                        tags = image.hashtags,
                                        imageUrl = image.thumbnailUrl,
                                        onClick = { onRecommendedImageClick(image) },
                                    )
                                }
                            }
                        }
                    }

                    val bottomShape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = bottomShape,
                                clip = false,
                            )
                            .clip(bottomShape)
                            .background(
                                ModeraTheme.colors.white,
                            ),
                    ) {
                        SelectedImagesSection(
                            images = selectedImages,
                            onRemoveClick = onSelectedImageRemoveClick,
                        )

                        ModeraIconTextButton(
                            text = stringResource(
                                R.string.document_create_button_label,
                            ),
                            icon = painterResource(
                                ModeraIcons.FileAdd,
                            ),
                            enabled = canCreateDocument,
                            onClick = onCreateDocumentClick,
                            buttonColor = if (canCreateDocument) {
                                ModeraTheme.colors.yellow700
                            } else {
                                ModeraTheme.colors.white
                            },
                            contentColor = if (canCreateDocument) {
                                ModeraTheme.colors.white
                            } else {
                                ModeraTheme.colors.gray500
                            },
                            borderColor = if (canCreateDocument) {
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

            DocumentCreateUiState.Creating -> {
                DocumentCreatingScreen(selectedImages = selectedImages)
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

@Preview(
    name = "DocumentCreateScreen",
    showBackground = true,
)
@Composable
private fun DocumentCreateScreenPreview(
    @PreviewParameter(
        DocumentCreateScreenPreviewParameterProvider::class,
    )
    previewData: DocumentCreateScreenPreviewData,
) {
    var uiState by remember(previewData) {
        mutableStateOf(previewData.uiState)
    }

    var selectedImages by remember(previewData) {
        mutableStateOf(previewData.selectedImages)
    }

    ModeraTheme {
        DocumentCreateScreen(
            uiState = uiState,
            selectedImages = selectedImages,
            hasSelectionChanged = false,
            onBackClick = {},
            onRefreshClick = {
                uiState = DocumentCreateUiState.Success(
                    recommendedImages =
                        previewDocumentCreateImages.filterNot { image ->
                            selectedImages.any { selectedImage ->
                                selectedImage.id == image.id
                            }
                        },
                )
            },
            onSelectedImageRemoveClick = { imageId ->
                val removedImage = selectedImages
                    .firstOrNull { image ->
                        image.id == imageId
                    }
                    ?: return@DocumentCreateScreen

                selectedImages = selectedImages.filterNot { image ->
                    image.id == imageId
                }

                val successState =
                    uiState as? DocumentCreateUiState.Success
                        ?: return@DocumentCreateScreen

                uiState = successState.copy(
                    recommendedImages =
                        listOf(removedImage) +
                                successState.recommendedImages.filterNot { image ->
                                    image.id == imageId
                                },
                )
            },
            onRecommendedImageClick = { image ->
                selectedImages = selectedImages + image

                val successState =
                    uiState as? DocumentCreateUiState.Success
                        ?: return@DocumentCreateScreen

                uiState = successState.copy(
                    recommendedImages =
                        successState.recommendedImages.filterNot {
                            it.id == image.id
                        },
                )
            },
            onCreateDocumentClick = {
                uiState = DocumentCreateUiState.Creating
            },
        )
    }
}