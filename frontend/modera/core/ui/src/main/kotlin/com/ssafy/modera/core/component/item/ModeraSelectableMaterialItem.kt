package com.ssafy.modera.core.component.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.modera.core.component.ModeraHashtags
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

/**
 * 스크린샷 관리 화면에서 사용하는 선택 가능한 자료 아이템.
 *
 * 일반 상태:
 * - 기본 테두리
 * - 선택 체크 표시 없음
 *
 * 편집 상태:
 * - 선택된 아이템은 강조 테두리와 체크 아이콘 표시
 * - 선택되지 않은 아이템은 연한 테두리 표시
 */
@Composable
fun ModeraSelectableMaterialItem(
    title: String,
    description: String,
    tags: List<String>,
    imageUrl: String,
    isEditing: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isEditing && isSelected -> {
            ModeraTheme.colors.yellow700
        }

        isEditing -> {
            ModeraTheme.colors.gray300
        }

        else -> {
            ModeraTheme.colors.gray500
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ItemShape)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = ItemShape,
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp, horizontal = 4.dp),
        ) {
            Text(
                text = title,
                style = ModeraTheme.typography.bodySB16,
                color = ModeraTheme.colors.gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))

                ModeraHashtags(
                    tags = tags,
                    color = ModeraTheme.colors.gray400,
                    style = ModeraTheme.typography.captionR12,
                )
            }
        }

        SelectableMaterialThumbnail(
            imageUrl = imageUrl,
            showSelection = isEditing && isSelected,
        )
    }
}

@Composable
private fun SelectableMaterialThumbnail(
    imageUrl: String,
    showSelection: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(ThumbnailShape)
            .background(ModeraTheme.colors.gray200),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        if (showSelection) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ModeraTheme.colors.yellow700),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        ModeraIcons.Check,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ModeraTheme.colors.white,
                )
            }
        }
    }
}

private val ItemShape = RoundedCornerShape(6.dp)
private val ThumbnailShape = RoundedCornerShape(4.dp)

@Preview(name = "일반 상태", showBackground = true)
@Composable
private fun ModeraSelectableMaterialItemPreview() {
    ModeraTheme {
        ModeraSelectableMaterialItem(
            title = "성심당 케이크 리스트",
            description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루",
            tags = listOf("기차", "예약", "KTX"),
            imageUrl = "",
            isEditing = false,
            isSelected = false,
            onClick = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Preview(name = "편집 선택 상태", showBackground = true)
@Composable
private fun ModeraSelectableMaterialItemSelectedPreview() {
    ModeraTheme {
        ModeraSelectableMaterialItem(
            title = "성심당 케이크 리스트",
            description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루",
            tags = listOf("기차", "예약", "KTX"),
            imageUrl = "",
            isEditing = true,
            isSelected = true,
            onClick = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}