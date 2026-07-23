package com.ssafy.modera.feature.categoryimages.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSummary
import com.ssafy.modera.feature.categoryimages.R

@Composable
internal fun AnalyzedImageItem(
    analyzedImageSummary: AnalyzedImageSummary,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ClickableSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(6.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White),
            ) {
                AsyncImage(
                    model = analyzedImageSummary.imageUrl,
                    contentDescription = stringResource(
                        R.string.category_image_content_description,
                        analyzedImageSummary.title,
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = ModeraTheme.colors.gray),
                    contentScale = ContentScale.Crop,
                )

                if (selectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = if (selected) {
                                    Color.Black.copy(alpha = 0.32f)
                                } else {
                                    Color.Transparent
                                },
                            ),
                    )

                    SelectionIndicator(
                        selected = selected,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    )
                }
            }

            AnalyzedImageInfo(
                title = analyzedImageSummary.title,
                hashtags = analyzedImageSummary.hashtags,
                modifier = Modifier.padding(
                    top = 8.dp,
                    start = 2.dp,
                    end = 2.dp,
                ),
            )
        }
    }
}

@Composable
private fun AnalyzedImageInfo(
    title: String,
    hashtags: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = ModeraTheme.typography.body2SemiBold,
            color = ModeraTheme.colors.typo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (hashtags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                hashtags
                    .take(3)
                    .forEach { hashtag ->
                        Text(
                            text = stringResource(
                                R.string.analyzed_image_hashtag,
                                hashtag.removePrefix("#"),
                            ),
                            modifier = Modifier.weight(
                                weight = 1f,
                                fill = false,
                            ),
                            style = ModeraTheme.typography.caption1,
                            color = ModeraTheme.colors.blue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }
        }
    }
}

@Composable
private fun SelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = if (selected) {
                    ModeraTheme.colors.blue
                } else {
                    Color.White.copy(alpha = 0.88f)
                },
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White,
            )
        }
    }
}

@Preview(
    name = "Analyzed Image Item - Default",
    showBackground = true,
    widthDp = 200,
)
@Composable
private fun AnalyzedImageItemDefaultPreview() {
    ModeraTheme {
        AnalyzedImageItem(
            analyzedImageSummary = previewAnalyzedImageSummary,
            selected = false,
            selectionMode = false,
            onClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(
    name = "Analyzed Image Item - Selection Mode",
    showBackground = true,
    widthDp = 200,
)
@Composable
private fun AnalyzedImageItemSelectionModePreview() {
    ModeraTheme {
        AnalyzedImageItem(
            analyzedImageSummary = previewAnalyzedImageSummary,
            selected = false,
            selectionMode = true,
            onClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(
    name = "Analyzed Image Item - Selected",
    showBackground = true,
    widthDp = 200,
)
@Composable
private fun AnalyzedImageItemSelectedPreview() {
    ModeraTheme {
        AnalyzedImageItem(
            analyzedImageSummary = previewAnalyzedImageSummary,
            selected = true,
            selectionMode = true,
            onClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

private val previewAnalyzedImageSummary = AnalyzedImageSummary(
    id = 1L,
    title = "삼성전자 주가 전망 및 투자 분석",
    imageUrl = "https://picsum.photos/seed/modera_stock/400/400",
    hashtags = listOf(
        "주식",
        "삼성전자",
        "투자정보",
    ),
)