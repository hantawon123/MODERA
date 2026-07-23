package com.ssafy.modera.feature.imagedetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.AnalyzedImageDetail
import com.ssafy.modera.core.model.KeyInfoItem
import com.ssafy.modera.feature.imagedetail.R

@Composable
internal fun ImageDetailSummarySection(
    summary: String,
    modifier: Modifier = Modifier,
) {
    ImageDetailSection(
        title = stringResource(R.string.image_detail_summary_title),
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_sparkle),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified,
            )
        },
        modifier = modifier,
    ) {
        Text(
            text = summary,
            style = ModeraTheme.typography.body2,
            color = ModeraTheme.colors.typo,
        )
    }
}

@Composable
internal fun ImageDetailKeyInfoSection(
    keyInfo: List<KeyInfoItem>,
    modifier: Modifier = Modifier,
) {
    ImageDetailSection(
        title = stringResource(R.string.image_detail_key_info_title),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            keyInfo.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = item.label,
                        modifier = Modifier.weight(0.35f),
                        style = ModeraTheme.typography.body2Medium,
                        color = ModeraTheme.colors.typo.copy(alpha = 0.6f),
                    )
                    Text(
                        text = item.value,
                        modifier = Modifier.weight(0.65f),
                        style = ModeraTheme.typography.body2SemiBold,
                        color = ModeraTheme.colors.typo,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ImageDetailOcrSection(
    ocrText: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ImageDetailSection(
        title = stringResource(R.string.image_detail_ocr_title),
        trailing = {
            ClickableSurface(
                onClick = onCopyClick,
                color = Color.Transparent,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = stringResource(R.string.image_detail_copy_ocr),
                    modifier = Modifier
                        .padding(4.dp)
                        .size(20.dp),
                    tint = ModeraTheme.colors.typo.copy(alpha = 0.6f),
                )
            }
        },
        modifier = modifier,
    ) {
        Text(
            text = ocrText,
            style = ModeraTheme.typography.body2,
            color = ModeraTheme.colors.typo,
        )
    }
}

@Composable
private fun ImageDetailSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ModeraTheme.colors.white)
            .padding(
                horizontal = 20.dp,
                vertical = 20.dp,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                icon?.invoke()
                Text(
                    text = title,
                    style = ModeraTheme.typography.subtitle3SemiBold,
                    color = ModeraTheme.colors.typo,
                )
            }
            trailing?.invoke()
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun ImageDetailContentSectionsPreview() {
    ModeraTheme {
        Column {
            ImageDetailSummarySection(summary = previewImageDetail.summary)
            ImageDetailKeyInfoSection(keyInfo = previewImageDetail.keyInfo)
            ImageDetailOcrSection(
                ocrText = previewImageDetail.ocrText,
                onCopyClick = {},
            )
        }
    }
}

private val previewImageDetail = AnalyzedImageDetail(
    id = 1L,
    title = "ASCII HACKATHON",
    imageUrl = "https://picsum.photos/seed/hackathon/800/1200",
    categoryId = 3L,
    categoryName = "개발",
    uploadedAt = "2026년 7월 17일 금요일 오후 7:23",
    hashtags = listOf("해커톤", "SW"),
    isFavorite = true,
    summary = "2026년도 제1회 대학 연합 해커톤 ASCII HACKATHON 포스터입니다.",
    keyInfo = listOf(
        KeyInfoItem("상품명", "C++ 프로그래밍 입문"),
        KeyInfoItem("판매처", "교보문고"),
        KeyInfoItem("가격", "32,000원"),
    ),
    ocrText = "ASCII HACKATHON\n2026. 1. 30. (금) - 1. 31. (토)",
)
