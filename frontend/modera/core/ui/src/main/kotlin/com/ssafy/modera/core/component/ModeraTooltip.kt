package com.ssafy.modera.core.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

/**
 * 기준 컴포넌트 주변에 짧은 안내 문구를 표시하는 툴팁.
 *
 * 기본적으로 기준 컴포넌트 아래에 표시하며, 아래 공간이 부족하면 위에 표시한다.
 * 이 컴포넌트는 [Box]와 같이 위치 기준이 될 부모 내부에서 호출해야 한다.
 *
 * @param expanded 툴팁 표시 여부
 * @param text 툴팁에 표시할 문구
 * @param onDismissRequest 외부 영역 또는 뒤로가기를 눌렀을 때 실행할 콜백
 * @param modifier 툴팁 콘텐츠에 적용할 Modifier
 * @param width 툴팁 너비
 * @param gap 기준 컴포넌트와 툴팁 사이 간격
 * @param horizontalOffset 기준 컴포넌트로부터의 가로 위치 보정값
 * @param shape 툴팁 모양
 * @param backgroundColor 툴팁 배경색
 * @param contentColor 툴팁 텍스트 색상
 * @param contentPadding 툴팁 내부 여백
 * @param properties Popup 동작 설정
 */
@Composable
fun ModeraTooltip(
    expanded: Boolean,
    text: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = ModeraTooltipDefaults.Width,
    gap: Dp = ModeraTooltipDefaults.Gap,
    horizontalOffset: Dp = ModeraTooltipDefaults.HorizontalOffset,
    shape: Shape = ModeraTooltipDefaults.Shape,
    backgroundColor: Color = ModeraTheme.colors.yellow700,
    contentColor: Color = ModeraTheme.colors.white,
    contentPadding: PaddingValues = ModeraTooltipDefaults.ContentPadding,
    properties: PopupProperties = ModeraTooltipDefaults.Properties,
) {
    if (!expanded) return

    val density = LocalDensity.current

    val gapPx = with(density) {
        gap.roundToPx()
    }

    val horizontalOffsetPx = with(density) {
        horizontalOffset.roundToPx()
    }

    val positionProvider = remember(
        gapPx,
        horizontalOffsetPx,
    ) {
        TooltipPositionProvider(
            gap = gapPx,
            horizontalOffset = horizontalOffsetPx,
        )
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        TooltipContent(
            text = text,
            width = width,
            shape = shape,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }
}

@Composable
private fun TooltipContent(
    text: String,
    width: Dp,
    shape: Shape,
    backgroundColor: Color,
    contentColor: Color,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(width)
            .background(
                color = backgroundColor,
                shape = shape,
            )
            .padding(contentPadding),
    ) {
        Text(
            text = text,
            style = ModeraTheme.typography.captionR12,
            color = contentColor,
        )
    }
}

private class TooltipPositionProvider(
    private val gap: Int,
    private val horizontalOffset: Int,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maximumX = (
                windowSize.width - popupContentSize.width
                ).coerceAtLeast(0)

        val preferredX = when (layoutDirection) {
            LayoutDirection.Ltr -> {
                anchorBounds.left + horizontalOffset
            }

            LayoutDirection.Rtl -> {
                anchorBounds.right -
                        popupContentSize.width -
                        horizontalOffset
            }
        }

        val x = preferredX.coerceIn(
            minimumValue = 0,
            maximumValue = maximumX,
        )

        val belowAnchorY = anchorBounds.bottom + gap
        val aboveAnchorY =
            anchorBounds.top - popupContentSize.height - gap

        val canPlaceBelow =
            belowAnchorY + popupContentSize.height <= windowSize.height

        val y = if (canPlaceBelow) {
            belowAnchorY
        } else {
            aboveAnchorY.coerceAtLeast(0)
        }

        return IntOffset(
            x = x,
            y = y,
        )
    }
}

object ModeraTooltipDefaults {

    val Width = 180.dp

    val Gap = 4.dp

    val HorizontalOffset = 12.dp

    val Shape = RoundedCornerShape(0.dp, 8.dp, 8.dp, 8.dp)

    val ContentPadding = PaddingValues(
        horizontal = 12.dp,
        vertical = 9.dp,
    )

    val Properties = PopupProperties(
        focusable = true,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
    )
}

@Preview(name = "ModeraTooltip", showBackground = true)
@Composable
private fun ModeraTooltipPreview() {
    ModeraTheme {
        TooltipContent(
            text = "스크린샷을 추가하거나 제거하면\nAI가 문서를 다시 정리해요.",
            width = ModeraTooltipDefaults.Width,
            shape = ModeraTooltipDefaults.Shape,
            backgroundColor = ModeraTheme.colors.yellow700,
            contentColor = ModeraTheme.colors.white,
            contentPadding = ModeraTooltipDefaults.ContentPadding,
            modifier = Modifier.padding(24.dp),
        )
    }
}