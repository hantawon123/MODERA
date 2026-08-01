package com.ssafy.modera.core.component.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.ui.R

/**
 * 분석 이미지 리스트 아이템 — 즐겨찾기/문서/일정 메타 row + 해시태그 row.
 */
@Composable
fun ModeraAnalyzedImageItem(
    title: String,
    description: String,
    tags: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    favorite: Boolean = false,
    isDocumented: Boolean = false,
    hasSchedule: Boolean = false,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(AnalyzedImageItemDefaults.ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(AnalyzedImageItemDefaults.TextImageSpacing),
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

                Spacer(modifier = Modifier.height(AnalyzedImageItemDefaults.TitleDescriptionSpacing))

                Text(
                    text = description,
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.gray500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (favorite || isDocumented || hasSchedule) {
                    Spacer(modifier = Modifier.height(AnalyzedImageItemDefaults.MetaItemSpacing))
                    AnalyzedImageMetaRow(
                        favorite = favorite,
                        isDocumented = isDocumented,
                        hasSchedule = hasSchedule,
                    )
                }

                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(AnalyzedImageItemDefaults.FooterSpacing))
                    Text(
                        text = tags.joinToString(separator = " ") { "#$it" },
                        style = ModeraTheme.typography.bodyR14,
                        color = ModeraTheme.colors.blue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (imageUrl != null) {
                Box(
                    modifier = Modifier
                        .size(AnalyzedImageItemDefaults.ThumbnailSize)
                        .clip(AnalyzedImageItemDefaults.ThumbnailShape)
                        .background(ModeraTheme.colors.gray200),
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = ModeraTheme.colors.gray200,
        )
    }
}

@Composable
private fun AnalyzedImageMetaRow(
    favorite: Boolean,
    isDocumented: Boolean,
    hasSchedule: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AnalyzedImageItemDefaults.MetaItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (favorite) {
            Icon(
                imageVector = ImageVector.vectorResource(ModeraIcons.StarFilled),
                contentDescription = stringResource(R.string.analyzed_image_item_favorite),
                modifier = Modifier.size(AnalyzedImageItemDefaults.MetaIconSize),
                tint = ModeraTheme.colors.yellow500,
            )

            if (isDocumented || hasSchedule) {
                Text(
                    text = stringResource(R.string.analyzed_image_item_meta_divider),
                    style = ModeraTheme.typography.captionR12,
                    color = ModeraTheme.colors.gray300,
                )
            }
        }

        if (isDocumented) {
            Text(
                text = stringResource(R.string.analyzed_image_item_document),
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.gray400,
            )
        }

        if (isDocumented && hasSchedule) {
            Text(
                text = stringResource(R.string.analyzed_image_item_meta_separator),
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.gray400,
            )
        }

        if (hasSchedule) {
            Text(
                text = stringResource(R.string.analyzed_image_item_schedule),
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.gray400,
            )
        }
    }
}

object AnalyzedImageItemDefaults {
    val ContentPadding = PaddingValues(vertical = 16.dp)
    val TextImageSpacing = 12.dp
    val TitleDescriptionSpacing = 6.dp
    val FooterSpacing = 12.dp
    val MetaItemSpacing = 4.dp
    val MetaIconSize = 14.dp
    val ThumbnailSize = 88.dp
    val ThumbnailShape = RoundedCornerShape(4.dp)
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ModeraAnalyzedImageItemAllMetaPreview() {
    ModeraTheme {
        ModeraAnalyzedImageItem(
            title = "성심당 케이크 리스트",
            description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
            tags = listOf("기차", "예약", "KTX"),
            imageUrl = "",
            favorite = true,
            isDocumented = true,
            hasSchedule = true,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ModeraAnalyzedImageItemDocumentSchedulePreview() {
    ModeraTheme {
        ModeraAnalyzedImageItem(
            title = "성심당 케이크 리스트",
            description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
            tags = listOf("기차", "예약", "KTX"),
            imageUrl = "",
            isDocumented = true,
            hasSchedule = true,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ModeraAnalyzedImageItemDocumentOnlyPreview() {
    ModeraTheme {
        ModeraAnalyzedImageItem(
            title = "성심당 케이크 리스트",
            description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
            tags = listOf("기차", "예약", "KTX"),
            imageUrl = "",
            favorite = true,
            isDocumented = true,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ModeraAnalyzedImageItemScheduleOnlyPreview() {
    ModeraTheme {
        ModeraAnalyzedImageItem(
            title = "성심당 케이크 리스트",
            description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
            tags = listOf("기차", "예약", "KTX"),
            imageUrl = "",
            favorite = true,
            hasSchedule = true,
            onClick = {},
        )
    }
}
