package com.ssafy.modera.core.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.category.CategorySortType

/**
 * 정렬 옵션 팝업. 부모 [Box] 기준으로 앵커되며, [T]에 정렬 enum 등 임의의 옵션 타입을 넘긴다.
 *
 * @param expanded true일 때만 표시
 * @param options 표시할 옵션 목록
 * @param selectedOption 현재 선택된 옵션
 * @param labelOf 옵션 → 표시 라벨
 */
@Composable
fun <T> ModeraSortPopup(
    expanded: Boolean,
    options: List<T>,
    selectedOption: T,
    labelOf: (T) -> String,
    onDismissRequest: () -> Unit,
    onOptionClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!expanded || options.isEmpty()) return

    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.roundToPx() }
    val positionProvider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = anchorBounds.right - popupContentSize.width
                val y = anchorBounds.bottom + gapPx
                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        SortPopupContent(
            options = options,
            selectedOption = selectedOption,
            labelOf = labelOf,
            onOptionClick = { option ->
                onOptionClick(option)
                onDismissRequest()
            },
            modifier = modifier,
        )
    }
}

@Composable
internal fun <T> SortPopupContent(
    options: List<T>,
    selectedOption: T,
    labelOf: (T) -> String,
    onOptionClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false,
            )
            .clip(RoundedCornerShape(12.dp))
            .background(ModeraTheme.colors.white)
            .padding(vertical = 6.dp),
    ) {
        options.forEachIndexed { index, option ->
            SortMenuItem(
                label = labelOf(option),
                selected = option == selectedOption,
                onClick = { onOptionClick(option) },
                modifier = Modifier.padding(vertical = 10.dp),
            )

            if (index != options.lastIndex) {
                HorizontalDivider(color = ModeraTheme.colors.gray50)
            }
        }
    }
}

@Composable
private fun SortMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixedFontSize = with(LocalDensity.current) { 14.dp.toSp() }
    val fixedLineHeight = with(LocalDensity.current) { 20.dp.toSp() }

    val textStyle = if (selected) {
        ModeraTheme.typography.bodySB14
    } else {
        ModeraTheme.typography.bodyR14
    }

    val contentColor = if (selected) {
        ModeraTheme.colors.yellow500
    } else {
        ModeraTheme.colors.gray700
    }

    ClickableSurface(
        onClick = onClick,
        modifier = modifier,
        color = ModeraTheme.colors.white,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = textStyle.copy(
                    fontSize = fixedFontSize,
                    lineHeight = fixedLineHeight,
                ),
                color = contentColor,
            )

            Icon(
                painter = painterResource(ModeraIcons.Check),
                contentDescription = null,
                tint = if (selected) {
                    ModeraTheme.colors.yellow500
                } else {
                    Color.Transparent
                },
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(
    name = "Category Sort Popup Content",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5,
)
@Composable
private fun SortPopupContentPreview() {
    ModeraTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            SortPopupContent(
                options = CategorySortType.entries,
                selectedOption = CategorySortType.NAME_ASC,
                labelOf = { it.label },
                onOptionClick = {},
            )
        }
    }
}
