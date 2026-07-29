package com.ssafy.modera.core.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

/**
 * 드롭다운 메뉴 아이템.
 *
 * @param icon 아이콘
 * @param label 버튼명
 * @param onClick 아이템 클릭 시 콜백
 * @param contentColor 아이콘·텍스트 색상. Unspecified면 [ModeraTheme.colors.gray900]
 */
@Immutable
data class ModeraDropdownMenuItem(
    val icon: Int,
    val label: String,
    val onClick: () -> Unit,
    val contentColor: Color = Color.Unspecified,
)

/**
 * 아이콘+텍스트 메뉴 아이템을 여러 개 받을 수 있는 공통 드롭다운.
 *
 * @param expanded true일 때만 메뉴를 표시
 * @param items 메뉴 아이템 목록
 * @param onDismissRequest 바깥 클릭/백 버튼으로 닫을 때
 */
@Composable
fun ModeraDropdownMenu(
    expanded: Boolean,
    items: List<ModeraDropdownMenuItem>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    offset: IntOffset = IntOffset.Zero,
    properties: PopupProperties = PopupProperties(
        focusable = true,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
    ),
) {
    if (!expanded || items.isEmpty()) return

    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        ModeraDropdownMenuContent(
            items = items,
            onItemClick = { item ->
                item.onClick()
                onDismissRequest()
            },
            modifier = modifier,
        )
    }
}

@Composable
fun ModeraDropdownMenuContent(
    items: List<ModeraDropdownMenuItem>,
    modifier: Modifier = Modifier,
    onItemClick: (ModeraDropdownMenuItem) -> Unit = { it.onClick() },
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = DropdownMenuDefaults.Elevation,
                shape = DropdownMenuDefaults.Shape,
                clip = false,
            )
            .clip(DropdownMenuDefaults.Shape)
            .background(ModeraTheme.colors.white)
            .padding(DropdownMenuDefaults.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(DropdownMenuDefaults.ItemSpacing),
    ) {
        items.forEach { item ->
            ModeraDropdownMenuItemRow(
                item = item,
                onClick = { onItemClick(item) },
            )
        }
    }
}

@Composable
private fun ModeraDropdownMenuItemRow(
    item: ModeraDropdownMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = item.contentColor.takeOrElse { ModeraTheme.colors.gray900 }

    Row(
        modifier = modifier
            .clip(DropdownMenuDefaults.ItemShape)
            .clickable(onClick = onClick)
            .padding(DropdownMenuDefaults.ItemPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DropdownMenuDefaults.IconTextSpacing),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(item.icon),
            contentDescription = null,
            modifier = Modifier.size(DropdownMenuDefaults.IconSize),
            tint = contentColor,
        )
        Text(
            text = item.label,
            style = ModeraTheme.typography.bodyR14,
            color = contentColor,
        )
    }
}

object DropdownMenuDefaults {
    val Shape = RoundedCornerShape(12.dp)
    val ItemShape = RoundedCornerShape(8.dp)
    val Elevation = 8.dp
    val ContentPadding = PaddingValues(
        horizontal = 8.dp,
        vertical = 8.dp,
    )
    val ItemPadding = PaddingValues(
        horizontal = 12.dp,
        vertical = 10.dp,
    )
    val ItemSpacing = 2.dp
    val IconTextSpacing = 8.dp
    val IconSize = 20.dp
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F3)
@Composable
private fun ModeraDropdownMenuContentPreview() {
    ModeraTheme {
        ModeraDropdownMenuContent(
            items = listOf(
                ModeraDropdownMenuItem(
                    icon = ModeraIcons.Refresh,
                    label = "재분석하기",
                    onClick = {},
                ),
                ModeraDropdownMenuItem(
                    icon = ModeraIcons.Trash,
                    label = "삭제하기",
                    contentColor = ModeraTheme.colors.red,
                    onClick = {},
                ),
            ),
            modifier = Modifier.padding(24.dp),
        )
    }
}
