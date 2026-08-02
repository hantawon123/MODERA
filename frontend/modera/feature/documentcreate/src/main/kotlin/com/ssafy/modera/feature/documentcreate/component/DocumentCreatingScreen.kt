package com.ssafy.modera.feature.documentcreate.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.feature.documentcreate.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun DocumentCreatingScreen(selectedImages: List<AnalyzedImage>) {
    val selectedImageIds = selectedImages.map(AnalyzedImage::id)

    var currentImageIndex by remember(selectedImageIds) {
        mutableIntStateOf(0)
    }

    LaunchedEffect(selectedImageIds) {
        currentImageIndex = 0

        if (selectedImages.size <= 1) {
            return@LaunchedEffect
        }

        while (isActive) {
            delay(DocumentCreatingItemDurationMillis.milliseconds)

            currentImageIndex =
                (currentImageIndex + 1) % selectedImages.size
        }
    }

    val currentImage = selectedImages.getOrNull(
        currentImageIndex,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            modifier = Modifier.weight(0.5f),
        )

        DocumentCreatingCharacter(
            characterPainter = painterResource(R.drawable.img_character_document_creating),
        )

        Spacer(
            modifier = Modifier.height(24.dp),
        )

        Text(
            text = stringResource(
                R.string.document_creating_title,
            ),
            style = ModeraTheme.typography.bodySB16,
            color = ModeraTheme.colors.gray900,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(12.dp),
        )

        Text(
            text = stringResource(
                R.string.document_creating_description,
            ),
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray500,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(64.dp),
        )

        AnimatedContent(
            targetState = currentImage,
            contentKey = { image ->
                image?.id
            },
            transitionSpec = {
                (
                        fadeIn() +
                                slideInVertically { height ->
                                    height / 3
                                }
                        ) togetherWith (
                        fadeOut() +
                                slideOutVertically { height ->
                                    -height / 3
                                }
                        )
            },
            label = "documentCreatingContent",
            modifier = Modifier.fillMaxWidth(),
        ) { image ->
            if (image != null) {
                DocumentCreatingImageContent(image = image)
            }
        }

        Spacer(
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DocumentCreatingCharacter(
    characterPainter: Painter,
    modifier: Modifier = Modifier,
) {
    val glowColor = ModeraTheme.colors.yellow500

    Box(
        modifier = modifier
            .size(220.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.2f),
                            glowColor.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                        radius = size.minDimension / 2f,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = characterPainter,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
        )
    }
}

@Composable
private fun DocumentCreatingImageContent(
    image: AnalyzedImage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                R.string.document_creating_image_title,
                image.title,
            ),
            style = ModeraTheme.typography.bodySB16,
            color = ModeraTheme.colors.gray900,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (image.hashtags.isNotEmpty()) {
            Spacer(
                modifier = Modifier.height(12.dp),
            )

            Text(
                text = image.hashtags.joinToString(
                    separator = " ",
                    transform = { hashtag ->
                        "#$hashtag"
                    },
                ),
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.gray500,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp),
        )

        Text(
            text = image.summary,
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray300,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(
            modifier = Modifier.height(12.dp),
        )
    }
}

private const val DocumentCreatingItemDurationMillis = 1_800L

@Preview(
    name = "Document Creating Screen",
    showBackground = true,
)
@Composable
private fun DocumentCreatingScreenPreview() {
    ModeraTheme {
        DocumentCreatingScreen(selectedImages = previewDocumentCreatingImages)
    }
}

private val previewDocumentCreatingImages =
    listOf(
        AnalyzedImage(
            id = 1L,
            title = "ASCII 해커톤",
            summary = "삼성전자 주최 도쿄이며, 2026년 7월 16일을 기준으로 진행되는 해커톤 모집 안내입니다." +
                    "삼성전자 주최 도쿄이며, 2026년 7월 16일을 기준으로 진행되는 해커톤 모집 안내입니다." +
                    "삼성전자 주최 도쿄이며, 2026년 7월 16일을 기준으로 진행되는 해커톤 모집 안내입니다." +
                    "삼성전자 주최 도쿄이며, 2026년 7월 16일을 기준으로 진행되는 해커톤 모집 안내입니다.",
            thumbnailUrl =
                "https://picsum.photos/seed/document-creating-1/300/300",
            hashtags = listOf(
                "해커톤",
                "SW",
                "개발",
            ),
            favorite = false,
        ),
        AnalyzedImage(
            id = 2L,
            title = "해커톤 참가 일정",
            summary = "참가 신청 기간과 행사 진행 일정, 장소 및 준비 사항을 정리한 자료입니다.",
            thumbnailUrl =
                "https://picsum.photos/seed/document-creating-2/300/300",
            hashtags = listOf(
                "일정",
                "참가신청",
                "서울",
            ),
            favorite = false,
        ),
        AnalyzedImage(
            id = 3L,
            title = "프로젝트 제출 안내",
            summary = "프로젝트 결과물 제출 방법과 발표 자료 작성 기준, 심사 항목을 안내합니다.",
            thumbnailUrl =
                "https://picsum.photos/seed/document-creating-3/300/300",
            hashtags = listOf(
                "프로젝트",
                "발표",
                "제출",
            ),
            favorite = false,
        ),
    )