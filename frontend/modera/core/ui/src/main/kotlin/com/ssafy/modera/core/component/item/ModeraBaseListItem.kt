package com.ssafy.modera.core.component.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

/**
 * 리스트 공통 베이스 아이템.
 *
 * @param title 제목
 * @param description 내용
 * @param imageUrl 썸네일 URL. null이면 텍스트가 fillMaxWidth
 * @param footer 하단 영역 (해시태그 / 메타 정보 등)
 */
@Composable
fun ModeraBaseListItem(
    title: String,
    description: String,
    onClick: (() -> Unit),
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    imageCountBadge: Int? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = true, onClick = onClick)
            .padding(ListItemDefaults.ContentPadding),
        horizontalArrangement = Arrangement.spacedBy(ListItemDefaults.TextImageSpacing),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = ModeraTheme.typography.bodySB16,
                color = ModeraTheme.colors.gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(ListItemDefaults.TitleDescriptionSpacing))

            Text(
                text = description,
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray500,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            footer()
        }

        if (imageUrl != null) {
            ListItemThumbnail(
                imageUrl = imageUrl,
                imageCountBadge = imageCountBadge,
            )
        }
    }
}

@Composable
private fun ListItemThumbnail(
    imageUrl: String,
    imageCountBadge: Int?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ListItemDefaults.ThumbnailSize)
            .clip(ListItemDefaults.ThumbnailShape)
            .background(ModeraTheme.colors.gray200),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        if (imageCountBadge != null && imageCountBadge > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(ModeraIcons.Images),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = ModeraTheme.colors.white,
                )
                Text(
                    text = imageCountBadge.toString(),
                    style = ModeraTheme.typography.captionR10,
                    color = ModeraTheme.colors.white,
                )
            }
        }
    }
}

object ListItemDefaults {
    val ContentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp,)
    val TextImageSpacing = 12.dp
    val TitleDescriptionSpacing = 6.dp
    val FooterSpacing = 14.dp
    val MetaItemSpacing = 10.dp
    val MetaIconSize = 16.dp
    val ThumbnailSize = 88.dp
    val ThumbnailShape = RoundedCornerShape(4.dp)
}
