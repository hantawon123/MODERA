package com.ssafy.modera.feature.analyzedimage.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.common.datetime.ModeraDateFormatter
import com.ssafy.modera.core.common.datetime.ModeraDateStyle
import com.ssafy.modera.core.component.ModeraConfirmDialog
import com.ssafy.modera.core.component.ModeraHashtags
import com.ssafy.modera.core.component.ModeraIconTextButton
import com.ssafy.modera.core.designsystem.component.IconButton
import com.ssafy.modera.core.designsystem.component.ModeraIconButtonDefaults
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import com.ssafy.modera.core.ui.ErrorScreen
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.analyzedimage.detail.component.AnalysisSummarySection
import com.ssafy.modera.feature.analyzedimage.detail.component.AnalyzedImageDetailActionItem
import com.ssafy.modera.feature.analyzedimage.detail.component.AnalyzedImageDetailSkeleton
import com.ssafy.modera.feature.analyzedimage.detail.component.AnalyzedImageDetailTopBar
import com.ssafy.modera.feature.analyzedimage.detail.component.CategoryLabel
import com.ssafy.modera.feature.analyzedimage.detail.component.ExtractedTextSection
import com.ssafy.modera.feature.analyzedimage.detail.component.ImageSection
import com.ssafy.modera.feature.analyzedimage.detail.component.KeyInformationSection
import com.ssafy.modera.feature.detail.AnalyzedImageDetailScreenPreviewData
import com.ssafy.modera.feature.detail.AnalyzedImageDetailScreenPreviewParameterProvider

private val TopBarTitleScrollThreshold = 96.dp

@Composable
internal fun AnalyzedImageDetailScreen(
    viewModel: AnalyzedImageDetailViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onCreateDocumentClick: (AnalyzedImage) -> Unit,
    onRelatedScheduleClick: () -> Unit,
    onRelatedDocumentClick: (Long, String) -> Unit,
    onRelatedImagesClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyzedImageDetailScreen(
        uiState = uiState,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onBackClick = onBackClick,
        onImageClick = onImageClick,
        onFavoriteClick = viewModel::toggleAnalyzedImageFavorite,
        onCreateDocumentClick = onCreateDocumentClick,
        onRelatedDocumentClick = onRelatedDocumentClick,
        onScheduleClick = onRelatedScheduleClick,
        onReanalyzeClick = viewModel::reanalyzeImage,
        onDeleteClick = {
            viewModel.deleteAnalyzedImage { onBackClick() }
        },
        onRelatedImagesClick = onRelatedImagesClick,
        modifier = modifier,
    )
}

@Composable
private fun AnalyzedImageDetailScreen(
    uiState: AnalyzedImageDetailUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onFavoriteClick: () -> Unit,
    onCreateDocumentClick: (AnalyzedImage) -> Unit,
    onScheduleClick: () -> Unit,
    onReanalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRelatedDocumentClick: (Long, String) -> Unit,
    onRelatedImagesClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember {
        mutableStateOf(false)
    }

    val scrollState = rememberScrollState()
    var dialog by remember {
        mutableStateOf<AnalyzedImageDetailDialog?>(null)
    }
    val title = if (
        uiState is AnalyzedImageDetailUiState.Success
    ) {
        uiState.image.title
    } else {
        ""
    }

    val titleScrollThresholdPx = with(LocalDensity.current) {
        TopBarTitleScrollThreshold.roundToPx()
    }

    val topBarTitle by remember(
        scrollState,
        title,
        titleScrollThresholdPx,
    ) {
        derivedStateOf {
            if (scrollState.value >= titleScrollThresholdPx) {
                title
            } else {
                ""
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white),
    ) {
        AnalyzedImageDetailTopBar(
            title = topBarTitle,
            menuExpanded = menuExpanded,
            onBackClick = onBackClick,
            onMoreClick = {
                if (uiState is AnalyzedImageDetailUiState.Success) {
                    menuExpanded = !menuExpanded
                }
            },
            onDismissMenu = {
                menuExpanded = false
            },
            onDocumentClick = {
                if (uiState is AnalyzedImageDetailUiState.Success) {
                    onCreateDocumentClick(
                        AnalyzedImage(
                            id = uiState.image.id,
                            title = uiState.image.title,
                            summary = uiState.image.summary,
                            thumbnailUrl = uiState.image.imageUrl,
                            hashtags = uiState.image.tags,
                        )
                    )
                }
            },
            onReanalyzeClick = {
                menuExpanded = false
                dialog = AnalyzedImageDetailDialog.REANALYZE
            },
            onDeleteClick = {
                menuExpanded = false
                dialog = AnalyzedImageDetailDialog.DELETE
            },
        )

        when (uiState) {
            AnalyzedImageDetailUiState.Loading -> {
                LoadingScreen(
                    modifier = Modifier.weight(1f),
                )
            }

            is AnalyzedImageDetailUiState.Success -> {
                AnalyzedImageDetailContent(
                    image = uiState.image,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    scrollState = scrollState,
                    onImageClick = onImageClick,
                    onFavoriteClick = onFavoriteClick,
                    onRelatedDocumentClick = onRelatedDocumentClick,
                    onScheduleClick = onScheduleClick,
                    onRelatedImagesClick = onRelatedImagesClick,
                    modifier = Modifier.weight(1f),
                )
            }

            is AnalyzedImageDetailUiState.Error -> {
                ErrorScreen(
                    message = stringResource(
                        R.string.analyzed_image_detail_load_error,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            is AnalyzedImageDetailUiState.Reanalyzing -> {
                AnalyzedImageDetailSkeleton(
                    scrollState = scrollState,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (uiState is AnalyzedImageDetailUiState.Success) {
            dialog?.let { activeDialog ->
                val confirmButtonColor = when (activeDialog) {
                    AnalyzedImageDetailDialog.REANALYZE -> {
                        ModeraTheme.colors.yellow800
                    }

                    AnalyzedImageDetailDialog.DELETE -> {
                        ModeraTheme.colors.red
                    }
                }

                ModeraConfirmDialog(
                    icon = painterResource(activeDialog.iconRes),
                    targetTitle = uiState.image.title,
                    title = stringResource(activeDialog.titleRes),
                    description = stringResource(activeDialog.descriptionRes),
                    confirmText = stringResource(activeDialog.confirmTextRes),
                    dismissText = stringResource(
                        R.string.analyzed_image_detail_dialog_cancel,
                    ),
                    confirmButtonColor = confirmButtonColor,
                    onConfirm = {
                        dialog = null

                        when (activeDialog) {
                            AnalyzedImageDetailDialog.REANALYZE -> {
                                onReanalyzeClick()
                            }

                            AnalyzedImageDetailDialog.DELETE -> {
                                onDeleteClick()
                            }
                        }
                    },
                    onDismiss = { dialog = null },
                )
            }
        }
    }
}

@Composable
private fun AnalyzedImageDetailContent(
    image: AnalyzedImageDetail,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    scrollState: ScrollState,
    onImageClick: (String) -> Unit,
    onFavoriteClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onRelatedDocumentClick: (Long, String) -> Unit,
    onRelatedImagesClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExtractedTextExpanded by rememberSaveable(image.id) {
        mutableStateOf(false)
    }

    val keyInformation = image.keyInformation
        .filter(String::isNotBlank)

    val extractedText = image.extractedTexts
        .filter(String::isNotBlank)
        .joinToString(separator = ", ")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(
                start = 24.dp,
                top = 8.dp,
                end = 24.dp,
                bottom = 24.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryLabel(
                category = image.category,
            )

            IconButton(
                painter = painterResource(
                    if (image.favorite) {
                        ModeraIcons.StarFilled
                    } else {
                        ModeraIcons.StarOutlined
                    },
                ),
                contentDescription = stringResource(
                    if (image.favorite) {
                        R.string.analyzed_image_detail_remove_favorite
                    } else {
                        R.string.analyzed_image_detail_add_favorite
                    },
                ),
                colors = ModeraIconButtonDefaults.iconButtonColors(
                    contentColor = ModeraTheme.colors.yellow800,
                ),
                size = 24.dp,
                onClick = onFavoriteClick,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = image.title,
            style = ModeraTheme.typography.titleB22,
            color = ModeraTheme.colors.gray900,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = ModeraDateFormatter.formatMillis(
                image.updatedAt,
                ModeraDateStyle.YEAR_MONTH_DAY_TIME
            ),
            style = ModeraTheme.typography.captionR12,
            color = ModeraTheme.colors.gray500,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (image.isDocumented) {
                AnalyzedImageDetailActionItem(
                    iconRes = ModeraIcons.FileDocument,
                    text = stringResource(
                        R.string.analyzed_image_detail_document,
                    ),
                    onClick = {
                        onRelatedDocumentClick(image.id, image.title)
                    },
                )
            }

            if (image.isCalendared) {
                AnalyzedImageDetailActionItem(
                    iconRes = ModeraIcons.CalendarNumber,
                    text = stringResource(
                        R.string.analyzed_image_detail_schedule,
                    ),
                    onClick = onScheduleClick,
                )
            }
        }

        if (image.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            ModeraHashtags(
                tags = image.tags,
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        AnalysisSummarySection(
            content = image.summary,
        )

        if (keyInformation.isNotEmpty()) {
            Spacer(modifier = Modifier.height(30.dp))

            KeyInformationSection(
                title = stringResource(
                    R.string.analyzed_image_detail_key_information_title,
                ),
                items = keyInformation,
            )
        }

        if (extractedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(30.dp))

            ExtractedTextSection(
                title = stringResource(
                    R.string.analyzed_image_detail_ocr_title,
                ),
                content = extractedText,
                expanded = isExtractedTextExpanded,
                onExpandClick = {
                    isExtractedTextExpanded = !isExtractedTextExpanded
                },
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        with(sharedTransitionScope) {
            ImageSection(
                imageUrl = image.imageUrl,
                onImageExpandClick = {
                    onImageClick(image.imageUrl)
                },
                modifier = Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "image-${image.imageUrl}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        ModeraIconTextButton(
            text = stringResource(
                R.string.analyzed_image_detail_related_images,
            ),
            icon = painterResource(ModeraIcons.FileSearch),
            onClick = { onRelatedImagesClick(image.id, image.title) },
            modifier = Modifier.fillMaxWidth(),
            buttonColor = ModeraTheme.colors.white,
            contentColor = ModeraTheme.colors.yellow500,
            borderColor = ModeraTheme.colors.yellow500,
        )
    }
}

@Preview(name = "AnalyzedImageDetailScreen", showBackground = true)
@Composable
private fun AnalyzedImageDetailScreenPreview(
    @PreviewParameter(AnalyzedImageDetailScreenPreviewParameterProvider::class)
    previewData: AnalyzedImageDetailScreenPreviewData,
) {
    var uiState by remember(previewData) {
        mutableStateOf(previewData.uiState)
    }

    ModeraTheme {
        SharedTransitionLayout {
            AnimatedVisibility(
                visible = true,
            ) {
                AnalyzedImageDetailScreen(
                    uiState = uiState,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility,
                    onBackClick = {},
                    onImageClick = {},
                    onFavoriteClick = {
                        val currentState = uiState

                        if (
                            currentState
                                    is AnalyzedImageDetailUiState.Success
                        ) {
                            uiState = currentState.copy(
                                image = currentState.image.copy(
                                    favorite =
                                        !currentState.image.favorite,
                                ),
                            )
                        }
                    },
                    onCreateDocumentClick = {},
                    onScheduleClick = {},
                    onReanalyzeClick = {},
                    onDeleteClick = {},
                    onRelatedDocumentClick = { _, _ -> },
                    onRelatedImagesClick = { _, _ -> },
                )
            }
        }
    }
}