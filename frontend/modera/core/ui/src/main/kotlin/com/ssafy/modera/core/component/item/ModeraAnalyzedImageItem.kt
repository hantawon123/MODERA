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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AnalyzedImageDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy년 MM월 dd일", Locale.KOREA)

/**
 * 분석 이미지 리스트 아이템 — 제목 옆 즐겨찾기 아이콘 + 문서/일정 메타 row + 해시태그 row.
 */
@Composable
fun ModeraAnalyzedImageItem(
    title: String,
    description: String,
    updatedAt: Long,
    tags: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    favorite: Boolean = false,
    isDocumented: Boolean = false,
    hasSchedule: Boolean = false,
) {
    val dateTime = remember(updatedAt) {
        updatedAt.toAnalyzedImageDateTime()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(AnalyzedImageItemDefaults.ContentPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (favorite) {
                Icon(
                    imageVector = ImageVector.vectorResource(ModeraIcons.StarFilled),
                    contentDescription = stringResource(R.string.analyzed_image_item_favorite),
                    modifier = Modifier.size(AnalyzedImageItemDefaults.MetaIconSize),
                    tint = ModeraTheme.colors.yellow500,
                )

                Spacer(Modifier.width(4.dp))
            }

            Text(
                text = title,
                style = ModeraTheme.typography.bodySB16,
                color = ModeraTheme.colors.gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dateTime.isNotEmpty()) {
                Text(
                    text = dateTime,
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.gray400,
                )
            }
            if (isDocumented || hasSchedule) {
                if (dateTime.isNotEmpty()) {
                    Spacer(Modifier.width(14.dp))
                }

                AnalyzedImageMetaRow(
                    isDocumented = isDocumented,
                    hasSchedule = hasSchedule,
                )
            }
        }

        Spacer(modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AnalyzedImageItemDefaults.TextImageSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags.joinToString(separator = " ") { "#$it" },
                        style = ModeraTheme.typography.bodyR14,
                        color = ModeraTheme.colors.blue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier.height(4.dp))
                Text(
                    text = description,
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.gray700,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
    }

    HorizontalDivider(
        thickness = 1.dp,
        color = ModeraTheme.colors.gray200,
    )
}

@Composable
private fun AnalyzedImageMetaRow(
    isDocumented: Boolean,
    hasSchedule: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var needsDivider = false

        if (isDocumented) {
            AnalyzedImageMetaItem(
                iconRes = ModeraIcons.FileDocument,
                label = stringResource(R.string.analyzed_image_item_document),
                iconTint = ModeraTheme.colors.gray400,
            )
            needsDivider = true
        }

        if (hasSchedule) {
            if (needsDivider) {
                AnalyzedImageMetaDivider()
            }
            AnalyzedImageMetaItem(
                iconRes = ModeraIcons.CalendarNumber,
                label = stringResource(R.string.analyzed_image_item_schedule),
                iconTint = ModeraTheme.colors.gray400,
            )
        }
    }
}

@Composable
private fun AnalyzedImageMetaItem(
    iconRes: Int,
    label: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
    textColor: Color = ModeraTheme.colors.gray400,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(AnalyzedImageItemDefaults.MetaIconSize),
            tint = iconTint,
        )

        Text(
            text = label,
            style = ModeraTheme.typography.captionM12,
            color = textColor,
            modifier = Modifier.padding(horizontal = AnalyzedImageItemDefaults.MetaItemSpacing),
        )
    }
}

@Composable
private fun AnalyzedImageMetaDivider() {
    Text(
        text = stringResource(R.string.analyzed_image_item_meta_divider),
        style = ModeraTheme.typography.captionR12,
        color = ModeraTheme.colors.gray300,
        modifier = Modifier.padding(horizontal = AnalyzedImageItemDefaults.MetaItemSpacing),
    )
}

private fun Long.toAnalyzedImageDateTime(): String {
    if (this <= 0L) return ""

    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(AnalyzedImageDateFormatter)
}

object AnalyzedImageItemDefaults {
    val ContentPadding = PaddingValues(vertical = 16.dp)
    val TextImageSpacing = 12.dp
    val TitleDescriptionSpacing = 6.dp
    val FooterSpacing = 12.dp
    val MetaItemSpacing = 4.dp
    val MetaIconSize = 14.dp
    val ThumbnailSize = 60.dp
    val ThumbnailShape = RoundedCornerShape(4.dp)
}

private const val PreviewUpdatedAt = 1_767_225_600_000L // 2026년 01월 01일

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ModeraAnalyzedImageItemAllMetaPreview() {
    ModeraTheme {
        ModeraAnalyzedImageItem(
            title = "성심당 케이크 리스트",
            description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
            updatedAt = PreviewUpdatedAt,
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
            updatedAt = PreviewUpdatedAt,
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
            updatedAt = PreviewUpdatedAt,
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
            updatedAt = PreviewUpdatedAt,
            tags = listOf("기차", "예약", "KTX"),
            imageUrl = "",
            favorite = false,
            hasSchedule = true,
            onClick = {},
        )
    }
}
