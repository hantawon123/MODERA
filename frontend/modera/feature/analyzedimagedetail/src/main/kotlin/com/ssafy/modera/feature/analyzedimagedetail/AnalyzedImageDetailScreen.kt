package com.ssafy.modera.feature.analyzedimagedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.IconButton
import com.ssafy.modera.core.designsystem.component.LoadingWheel
import com.ssafy.modera.core.designsystem.component.ModeraIconButtonDefaults
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageCategory
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import com.ssafy.modera.feature.analyzedimagedetail.component.AnalysisSummarySection
import com.ssafy.modera.feature.analyzedimagedetail.component.CategoryLabel
import com.ssafy.modera.feature.analyzedimagedetail.component.DetailAppBar
import com.ssafy.modera.feature.analyzedimagedetail.component.ErrorScreen
import com.ssafy.modera.feature.analyzedimagedetail.component.ImageSection
import com.ssafy.modera.feature.analyzedimagedetail.component.OcrTextSection
import com.ssafy.modera.feature.analyzedimagedetail.component.RelatedImagesButton

@Composable
fun AnalyzedImageDetailScreen(
    uiState: AnalyzedImageDetailUiState,
    onBackClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onDocumentClick: () -> Unit,
    onReanalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRelatedImagesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        AnalyzedImageDetailUiState.Loading -> {
            LoadingWheel(modifier = modifier)
        }

        is AnalyzedImageDetailUiState.Success -> {
            AnalyzedImageDetailScreen(
                image = uiState.image,
                onBackClick = onBackClick,
                onImageClick = onImageClick,
                onDocumentClick = onDocumentClick,
                onReanalyzeClick = onReanalyzeClick,
                onDeleteClick = onDeleteClick,
                onRelatedImagesClick = onRelatedImagesClick,
                modifier = modifier,
            )
        }

        is AnalyzedImageDetailUiState.Error -> {
            ErrorScreen(
                onBackClick = onBackClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AnalyzedImageDetailScreen(
    image: AnalyzedImageDetail,
    onBackClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onDocumentClick: () -> Unit,
    onReanalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRelatedImagesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            DetailAppBar(
                menuExpanded = menuExpanded,
                onBackClick = onBackClick,
                onMoreClick = {
                    menuExpanded = true
                },
                onDismissMenu = {
                    menuExpanded = false
                },
                onDocumentClick = {
                    menuExpanded = false
                    onDocumentClick()
                },
                onReanalyzeClick = {
                    menuExpanded = false
                    onReanalyzeClick()
                },
                onDeleteClick = {
                    menuExpanded = false
                    onDeleteClick()
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 32.dp,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryLabel(
                        category = image.categories.name,
                    )

                    IconButton(
                        painter = painterResource(ModeraIcons.Star),
                        contentDescription = "즐겨찾기 토글",
                        colors = ModeraIconButtonDefaults.iconButtonColors(
                            contentColor = ModeraTheme.colors.yellow800,
                        ),
                        onClick = {},
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = image.title,
                    style = ModeraTheme.typography.titleB22.copy(
                        color = ModeraTheme.colors.gray900,
                    ),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = image.createdAt,
                    style = ModeraTheme.typography.captionR12.copy(
                        color = ModeraTheme.colors.gray500,
                    ),
                )

                if (image.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))

                    // todo: Tags
                }

                Spacer(modifier = Modifier.height(30.dp))

                AnalysisSummarySection(image.summary)

                val ocrText = image.ocr
                    ?.rawText
                    .orEmpty()

                if (ocrText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(30.dp))

                    OcrTextSection(
                        title = "추출된 텍스트",
                        content = ocrText,
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                ImageSection(
                    imageUrl = image.imageUrl,
                    onImageExpandClick = {
                        onImageClick(image.imageUrl)
                    },
                )

                Spacer(modifier = Modifier.height(30.dp))

                RelatedImagesButton(
                    onClick = onRelatedImagesClick,
                )
            }
        }
    }
}

@Preview(
    name = "AnalyzedImageDetailScreen",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 412,
    heightDp = 915,
)
@Composable
private fun AnalyzedImageDetailScreenPreview() {
    ModeraTheme {
        AnalyzedImageDetailScreen(
            image = previewAnalyzedImageDetail,
            onBackClick = {},
            onImageClick = {},
            onDocumentClick = {},
            onReanalyzeClick = {},
            onDeleteClick = {},
            onRelatedImagesClick = {},
        )
    }
}

private val previewAnalyzedImageDetail = AnalyzedImageDetail(
    id = 1L,
    fileName = "hackathon.png",
    status = ImageAnalysisStatus.COMPLETED,
    favorite = false,
    title = "2026 대학생 연합 해커톤 모집",
    summary = """
        대학생과 취업 준비생을 대상으로 진행되는 연합 해커톤 모집 공고입니다.
        참가자는 팀을 구성해 서비스 아이디어를 기획하고 구현하며,
        우수 팀에는 상금과 후속 지원이 제공됩니다.
    """.trimIndent(),
    ocr = null,
    tags = listOf(
        "해커톤",
        "대학생",
        "공모전",
    ),
    categories = AnalyzedImageCategory(
        categoryId = 1L,
        name = "대회/공모전",
    ),
    imageUrl = "https://picsum.photos/600/800",
    createdAt = "2026.07.29",
    updatedAt = "2026.07.29",
)