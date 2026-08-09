package com.ssafy.modera.core.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

/**
 * icon + text 버튼
 * default : border 버튼
 *
 * @param text 버튼에 표시할 텍스트
 * @param icon 버튼 왼쪽에 표시할 아이콘
 * @param onClick 버튼 클릭 시 실행할 콜백
 * @param modifier 버튼 레이아웃 modifier
 * @param buttonColor 버튼 배경색
 * @param contentColor 아이콘과 텍스트 색상
 * @param borderColor 테두리 색상
 * @param isDashedBorder true면 점선 테두리, false면 실선 테두리
 * @param enabled 버튼 활성화 여부
 * @param iconContentDescription 아이콘 접근성 설명
 */
@Composable
fun ModeraIconTextButton(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: Color = ModeraTheme.colors.white,
    contentColor: Color = ModeraTheme.colors.gray500,
    borderColor: Color = ModeraTheme.colors.gray400,
    isDashedBorder: Boolean = false,
    enabled: Boolean = true,
    iconContentDescription: String? = null,
) {
    val shape = IconTextButtonDefaults.Shape

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
            )
            .background(
                color = buttonColor,
                shape = shape,
            )
            .iconTextButtonBorder(
                color = borderColor,
                shape = shape,
                width = IconTextButtonDefaults.BorderWidth,
                dashed = isDashedBorder,
            )
            .padding(vertical = IconTextButtonDefaults.VerticalPadding)
            .semantics { role = Role.Button },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = iconContentDescription,
            modifier = Modifier.size(IconTextButtonDefaults.IconSize),
            tint = contentColor,
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = IconTextButtonDefaults.IconTextSpacing),
            style = ModeraTheme.typography.bodySB16,
            color = contentColor,
        )
    }
}

private fun Modifier.iconTextButtonBorder(
    color: Color,
    shape: Shape,
    width: Dp,
    dashed: Boolean,
): Modifier {
    if (!dashed) {
        return border(
            width = width,
            color = color,
            shape = shape,
        )
    }

    return drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        val stroke = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(
                    IconTextButtonDefaults.DashLength.toPx(),
                    IconTextButtonDefaults.DashGap.toPx(),
                ),
            ),
        )

        when (outline) {
            is Outline.Rounded -> {
                val path = Path().apply { addRoundRect(outline.roundRect) }
                drawPath(
                    path = path,
                    color = color,
                    style = stroke,
                )
            }

            is Outline.Rectangle -> drawRect(
                color = color,
                style = stroke,
            )

            is Outline.Generic -> drawPath(
                path = outline.path,
                color = color,
                style = stroke,
            )
        }
    }
}

object IconTextButtonDefaults {
    val Shape = RoundedCornerShape(10.dp)
    val IconSize = 20.dp
    val IconTextSpacing = 8.dp
    val BorderWidth = 1.dp
    val VerticalPadding = 14.dp
    val DashLength = 4.dp
    val DashGap = 4.dp
}

@Preview(showBackground = true)
@Composable
private fun ModeraIconTextButtonFilledPreview() {
    ModeraTheme {
        ModeraIconTextButton(
            text = "문서화하기",
            icon = painterResource(ModeraIcons.FileAdd),
            onClick = {},
            buttonColor = ModeraTheme.colors.yellow500,
            contentColor = ModeraTheme.colors.white,
            borderColor = ModeraTheme.colors.yellow500,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModeraIconTextButtonOutlinedPrimaryPreview() {
    ModeraTheme {
        ModeraIconTextButton(
            text = "연관 자료 보러 가기",
            icon = painterResource(ModeraIcons.FileSearch),
            onClick = {},
            buttonColor = ModeraTheme.colors.white,
            contentColor = ModeraTheme.colors.yellow500,
            borderColor = ModeraTheme.colors.yellow500,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModeraIconTextButtonDashedBorderPreview() {
    ModeraTheme {
        ModeraIconTextButton(
            text = "문서화하기",
            icon = painterResource(ModeraIcons.FileAdd),
            onClick = {},
            buttonColor = ModeraTheme.colors.white,
            contentColor = ModeraTheme.colors.gray500,
            borderColor = ModeraTheme.colors.gray300,
            isDashedBorder = true,
        )
    }
}
