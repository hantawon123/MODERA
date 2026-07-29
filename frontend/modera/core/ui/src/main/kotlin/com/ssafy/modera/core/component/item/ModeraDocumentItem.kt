package com.ssafy.modera.core.component.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

/**
 * 문서 아이템 — 베이스 + 원본 이미지 개수 / 업데이트 시각.
 *
 * @param originalImageCountText 예: "원본 8개"
 * @param updatedAtText 예: "7월 27일 업데이트"
 */
@Composable
fun ModeraDocumentItem(
    title: String,
    description: String,
    originalImageCountText: String,
    updatedAtText: String,
    onClick: (() -> Unit),
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    imageCountBadge: Int? = null,
) {
    ModeraBaseListItem(
        title = title,
        description = description,
        modifier = modifier,
        imageUrl = imageUrl,
        imageCountBadge = imageCountBadge,
        onClick = onClick,
    ) {
        Spacer(modifier = Modifier.height(ListItemDefaults.FooterSpacing))
        DocumentMetaRow(
            originalImageCountText = originalImageCountText,
            updatedAtText = updatedAtText,
        )
    }
}

@Composable
private fun DocumentMetaRow(
    originalImageCountText: String,
    updatedAtText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ListItemDefaults.MetaItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DocumentMetaChip(
            icon = ModeraIcons.Image,
            text = originalImageCountText,
        )
        DocumentMetaChip(
            icon = ModeraIcons.Clock,
            text = updatedAtText,
        )
    }
}

@Composable
private fun DocumentMetaChip(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(icon),
            contentDescription = null,
            modifier = Modifier.size(ListItemDefaults.MetaIconSize),
            tint = ModeraTheme.colors.gray400,
        )
        Text(
            text = text,
            style = ModeraTheme.typography.captionR12,
            color = ModeraTheme.colors.gray400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ModeraDocumentItemPreview() {
    ModeraTheme {
        ModeraDocumentItem(
            title = "오사카 3박 4일 여행 계획",
            description = "항공권, 숙소, 맛집 정보를 분석해 날짜별 일정과 추천 코스로 정리했어요. " +
                    "항공권, 숙소, 맛집 정보를 분석해 날짜별 일정과 추천 코스로 정리했어요.",
            originalImageCountText = "원본 8개",
            updatedAtText = "7월 27일 업데이트",
            imageCountBadge = 4,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ModeraDocumentItemWithImagePreview() {
    ModeraTheme {
        ModeraDocumentItem(
            title = "오사카 3박 4일 여행 계획",
            description = "항공권, 숙소, 맛집 정보를 분석해 날짜별 일정과 추천 코스로 정리했어요.",
            originalImageCountText = "원본 8개",
            updatedAtText = "7월 27일 업데이트",
            imageUrl = "",
            onClick = {},
        )
    }
}
