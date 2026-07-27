package com.ssafy.modera.feature.imagedetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.designsystem.component.LoadingWheel
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageCategory
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageOcr
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import com.ssafy.modera.feature.imagedetail.component.ImageDetailActionBarAnimated
import com.ssafy.modera.feature.imagedetail.component.ImageDetailHeroSection
import com.ssafy.modera.feature.imagedetail.component.ImageDetailOcrSection
import com.ssafy.modera.feature.imagedetail.component.ImageDetailOverflowMenu
import com.ssafy.modera.feature.imagedetail.component.ImageDetailSummarySection
import com.ssafy.modera.feature.imagedetail.component.ImageDetailTopOverlay

/**
 * ViewModel 연결 및 UiState 분기 Screen
 */
@Composable
fun ImageDetailScreen(
    onBackClick: () -> Unit,
    onCategoryClick: (Long) -> Unit,
    onReanalyzeClick: (Long) -> Unit,
    onRelatedMaterialsClick: (Long) -> Unit,
    onCopyOcrTextClick: (String) -> Unit,
    onViewImageInfoClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is ImageDetailUiState.Loading -> {
            ImageDetailLoadingScreen(
                modifier = modifier,
            )
        }

        is ImageDetailUiState.Success -> {
            ImageDetailScreen(
                image = state.image,
                onBackClick = onBackClick,
                onCategoryClick = onCategoryClick,
                onReanalyzeClick = onReanalyzeClick,
                onRelatedMaterialsClick = onRelatedMaterialsClick,
                onCopyOcrTextClick = onCopyOcrTextClick,
                onViewImageInfoClick = onViewImageInfoClick,
                onDeleteClick = onDeleteClick,
                onFavoriteClick = onFavoriteClick,
                modifier = modifier,
            )
        }

        is ImageDetailUiState.Error -> {
            ImageDetailErrorScreen(
                onBackClick = onBackClick,
                modifier = modifier,
            )
        }
    }
}

/**
 * 실제 이미지 상세 UI
 */
@Composable
fun ImageDetailScreen(
    image: AnalyzedImageDetail,
    onBackClick: () -> Unit,
    onCategoryClick: (Long) -> Unit,
    onReanalyzeClick: (Long) -> Unit,
    onRelatedMaterialsClick: (Long) -> Unit,
    onCopyOcrTextClick: (String) -> Unit,
    onViewImageInfoClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember {
        mutableStateOf(false)
    }

    var showOverlay by remember {
        mutableStateOf(false)
    }

    var showOverflowMenu by remember {
        mutableStateOf(false)
    }

    val scrollState = rememberScrollState()

    val showActionBar by remember {
        derivedStateOf {
            scrollState.value > ACTION_BAR_SCROLL_THRESHOLD
        }
    }

    LaunchedEffect(Unit) {
        entered = true
    }

    AnimatedVisibility(
        visible = entered,
        modifier = modifier.fillMaxSize(),
        enter = slideInVertically(
            animationSpec = tween(ANIMATION_DURATION),
            initialOffsetY = { fullHeight ->
                fullHeight
            },
        ) + fadeIn(
            animationSpec = tween(ANIMATION_DURATION),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ModeraTheme.colors.gray),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                ImageDetailHeroSection(
                    imageUrl = image.imageUrl,
                    title = image.title,
                    showOverlay = showOverlay,
                    categoryName = image.categories.name,
                    uploadedAt = image.updatedAt,
                    hashtags = image.tags,
                    isFavorite = image.favorite,
                    onImageClick = {
                        showOverlay = !showOverlay
                    },
                    onCategoryClick = {
                        onCategoryClick(image.categories.categoryId)
                    },
                    onFavoriteClick = {
                        onFavoriteClick(image.id)
                    },
                )

                ImageDetailSummarySection(
                    summary = image.summary,
                )

                ImageDetailOcrSection(
                    analyzedImageOcr = image.ocr,
                    onCopyClick = {
                        onCopyOcrTextClick(
                            image.ocr?.rawText.orEmpty(),
                        )
                    },
                )

                Spacer(
                    modifier = Modifier.height(88.dp),
                )
            }

            ImageDetailTopOverlay(
                visible = showOverlay,
                onBackClick = onBackClick,
                onReanalyzeClick = {
                    onReanalyzeClick(image.id)
                },
                onMoreClick = {
                    showOverflowMenu = true
                },
                modifier = Modifier.align(Alignment.TopCenter),
            )

            ImageDetailOverflowMenu(
                expanded = showOverflowMenu,
                onDismissRequest = {
                    showOverflowMenu = false
                },
                onCopyTextClick = {
                    showOverflowMenu = false

                    onCopyOcrTextClick(
                        image.ocr?.rawText.orEmpty(),
                    )
                },
                onViewInfoClick = {
                    showOverflowMenu = false
                    onViewImageInfoClick(image.id)
                },
                onDeleteClick = {
                    showOverflowMenu = false
                    onDeleteClick(image.id)
                },
            )

            ImageDetailActionBarAnimated(
                visible = showActionBar,
                onReanalyzeClick = {
                    onReanalyzeClick(image.id)
                },
                onRelatedMaterialsClick = {
                    onRelatedMaterialsClick(image.id)
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ImageDetailLoadingScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.gray),
        contentAlignment = Alignment.Center,
    ) {
        LoadingWheel()
    }
}

@Composable
private fun ImageDetailErrorScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.gray),
    ) {
        ImageDetailTopOverlay(
            visible = true,
            onBackClick = onBackClick,
            onReanalyzeClick = {},
            onMoreClick = {},
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Text(
            text = "이미지 정보를 불러오지 못했습니다.",
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.typo,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Preview(
    name = "Image Detail",
    showBackground = true,
)
@Composable
private fun ImageDetailScreenPreview() {
    ModeraTheme {
        ImageDetailScreen(
            image = previewImageDetail,
            onBackClick = {},
            onCategoryClick = {},
            onReanalyzeClick = {},
            onRelatedMaterialsClick = {},
            onCopyOcrTextClick = {},
            onViewImageInfoClick = {},
            onDeleteClick = {},
            onFavoriteClick = {},
        )
    }
}

@Preview(
    name = "Image Detail Loading",
    showBackground = true,
)
@Composable
private fun ImageDetailLoadingScreenPreview() {
    ModeraTheme {
        ImageDetailLoadingScreen()
    }
}

@Preview(
    name = "Image Detail Error",
    showBackground = true,
)
@Composable
private fun ImageDetailErrorScreenPreview() {
    ModeraTheme {
        ImageDetailErrorScreen(
            onBackClick = {},
        )
    }
}

private val previewImageDetail = AnalyzedImageDetail(
    id = 1L,
    fileName = "ascii_hackathon_poster.jpg",
    status = ImageAnalysisStatus.COMPLETED,
    favorite = true,
    title = "ASCII HACKATHON",
    summary = "2026년도 제1회 대학 연합 해커톤 ASCII HACKATHON 포스터입니다.",
    ocr = AnalyzedImageOcr(
        rawText = """
            ASCII HACKATHON
            2026. 1. 30. (금) - 1. 31. (토)
        """.trimIndent(),
        refinedText = null,
        language = "ko",
        confidence = 0.99,
    ),
    tags = listOf(
        "해커톤",
        "SW",
    ),
    categories = AnalyzedImageCategory(
        categoryId = 3L,
        name = "개발",
    ),
    imageUrl = "https://picsum.photos/seed/hackathon/800/1200",
    createdAt = "2026-07-17T19:23:00",
    updatedAt = "2026-07-17T19:23:00",
)

private const val ACTION_BAR_SCROLL_THRESHOLD = 120
private const val ANIMATION_DURATION = 320