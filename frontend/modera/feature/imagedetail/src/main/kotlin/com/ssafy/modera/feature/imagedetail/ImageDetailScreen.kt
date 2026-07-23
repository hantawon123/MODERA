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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.AnalyzedImageDetail
import com.ssafy.modera.feature.imagedetail.component.ImageDetailActionBarAnimated
import com.ssafy.modera.feature.imagedetail.component.ImageDetailHeroSection
import com.ssafy.modera.feature.imagedetail.component.ImageDetailKeyInfoSection
import com.ssafy.modera.feature.imagedetail.component.ImageDetailOcrSection
import com.ssafy.modera.feature.imagedetail.component.ImageDetailOverflowMenu
import com.ssafy.modera.feature.imagedetail.component.ImageDetailSummarySection
import com.ssafy.modera.feature.imagedetail.component.ImageDetailTopOverlay

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
    var entered by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val showActionBar by remember {
        derivedStateOf { scrollState.value > 120 }
    }

    LaunchedEffect(Unit) {
        entered = true
    }

    AnimatedVisibility(
        visible = entered,
        modifier = modifier.fillMaxSize(),
        enter = slideInVertically(
            animationSpec = tween(320),
            initialOffsetY = { fullHeight -> fullHeight },
        ) + fadeIn(animationSpec = tween(320)),
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
                    categoryName = image.categoryName,
                    uploadedAt = image.uploadedAt,
                    hashtags = image.hashtags,
                    isFavorite = image.isFavorite,
                    onImageClick = { showOverlay = !showOverlay },
                    onCategoryClick = { onCategoryClick(image.categoryId) },
                    onFavoriteClick = { onFavoriteClick(image.id) },
                )

                ImageDetailSummarySection(summary = image.summary)

                ImageDetailKeyInfoSection(keyInfo = image.keyInfo)

                ImageDetailOcrSection(
                    ocrText = image.ocrText,
                    onCopyClick = { onCopyOcrTextClick(image.ocrText) },
                )

                Spacer(modifier = Modifier.height(88.dp))
            }

            ImageDetailTopOverlay(
                visible = showOverlay,
                onBackClick = onBackClick,
                onReanalyzeClick = { onReanalyzeClick(image.id) },
                onMoreClick = { showOverflowMenu = true },
                modifier = Modifier.align(Alignment.TopCenter),
            )

            ImageDetailOverflowMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false },
                onCopyTextClick = { onCopyOcrTextClick(image.ocrText) },
                onViewInfoClick = { onViewImageInfoClick(image.id) },
                onDeleteClick = { onDeleteClick(image.id) },
            )

            ImageDetailActionBarAnimated(
                visible = showActionBar,
                onReanalyzeClick = { onReanalyzeClick(image.id) },
                onRelatedMaterialsClick = { onRelatedMaterialsClick(image.id) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Preview(
    name = "Image Detail Screen",
    showBackground = true,
    heightDp = 800,
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

internal val previewImageDetail = AnalyzedImageDetail(
    id = 1L,
    title = "ASCII HACKATHON",
    imageUrl = "https://picsum.photos/seed/hackathon/800/1200",
    categoryId = 3L,
    categoryName = "개발",
    uploadedAt = "2026년 7월 17일 금요일 오후 7:23",
    hashtags = listOf("해커톤", "SW"),
    isFavorite = true,
    summary = "2026년도 제1회 대학 연합 해커톤 ASCII HACKATHON 포스터입니다. " +
        "전국 주요 대학의 우수한 인재들이 모여 혁신적인 SW 기술을 개발하는 행사로, " +
        "무박 2일간 진행됩니다.",
    keyInfo = listOf(
        com.ssafy.modera.core.model.KeyInfoItem("상품명", "C++ 프로그래밍 입문"),
        com.ssafy.modera.core.model.KeyInfoItem("판매처", "교보문고"),
        com.ssafy.modera.core.model.KeyInfoItem("가격", "32,000원"),
    ),
    ocrText = "2026년도 제1회 대학 연합 해커톤\n" +
        "ASCII HACKATHON\n" +
        "2026. 1. 30. (금) - 1. 31. (토)\n" +
        "무박 2일\n" +
        "전국 주요 대학의 우수한 인재들이 모여\n" +
        "혁신적인 SW 기술을 개발하는 해커톤",
)
