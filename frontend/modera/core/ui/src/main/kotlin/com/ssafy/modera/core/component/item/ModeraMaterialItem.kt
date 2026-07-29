package com.ssafy.modera.core.component.item

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ssafy.modera.core.component.ModeraHashtags
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

/**
 * 자료 아이템 — 베이스 + 해시태그 row.
 */
@Composable
fun ModeraMaterialItem(
    title: String,
    description: String,
    tags: List<String>,
    onClick: (() -> Unit),
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    imageCountBadge: Int? = null,
    onTagClick: ((String) -> Unit)? = null,
) {
    ModeraBaseListItem(
        title = title,
        description = description,
        modifier = modifier,
        imageUrl = imageUrl,
        imageCountBadge = imageCountBadge,
        onClick = onClick,
    ) {
        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(ListItemDefaults.FooterSpacing))
            ModeraHashtags(
                tags = tags,
                color = ModeraTheme.colors.gray400,
                style = ModeraTheme.typography.captionR12,
                onTagClick = onTagClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ModeraMaterialItemWithImagePreview() {
    ModeraTheme {
        ModeraMaterialItem(
            title = "성심당 케이크 리스트",
            description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다. 샤인머스켓 시루, 귤 시루, 맛있겠다.",
            tags = listOf("기차", "예약", "KTX"),
            imageUrl = "",
            onClick = {},
        )
    }
}