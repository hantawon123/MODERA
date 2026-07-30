package com.ssafy.modera.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.designsystem.theme.pretendardFontFamily

/**
 * Modera 커스텀 바텀 네비게이션.
 */
@Composable
fun ModeraBottomNavigation(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ModeraTheme.colors.white)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ModeraTheme.colors.gray200),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * 일반 탭 아이템 (아이콘 + 라벨). 선택 시 라벨 뒤에 기울어진 형광펜 하이라이트를 그린다.
 */
@Composable
fun RowScope.ModeraBottomNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        ModeraTheme.colors.yellow800
    } else {
        ModeraTheme.colors.gray500
    }
    val highlightColor = ModeraTheme.colors.yellow500.copy(alpha = 0.35f)

    Column(
        modifier = modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        HighlighterLabel(
            text = label,
            selected = selected,
            color = contentColor,
            highlightColor = highlightColor,
        )
    }
}

/**
 * 중앙 주요 액션 버튼 (노란 squircle + 흰색 +).
 */
@Composable
fun RowScope.ModeraBottomNavigationAction(
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ModeraTheme.colors.yellow500),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = contentDescription,
                tint = ModeraTheme.colors.white,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun HighlighterLabel(
    text: String,
    selected: Boolean,
    color: Color,
    highlightColor: Color,
) {
    val highlightProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "highlighterExpand",
    )
    val density = LocalDensity.current
    val fixedFontSize = with(density) { 10.dp.toSp() }
    val fixedLineHeight = with(density) { 16.dp.toSp() }

    Text(
        text = text,
        color = color,
        style = TextStyle(
            fontSize = fixedFontSize,
            lineHeight = fixedLineHeight,
            fontFamily = pretendardFontFamily,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        ),
        modifier = if (highlightProgress > 0f) {
            Modifier.drawBehind {
                val fullWidth = size.width + 8.dp.toPx()
                val highlightWidth = fullWidth * highlightProgress
                val highlightHeight = size.height - 2.dp.toPx()
                rotate(degrees = -10f) {
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(
                            x = (size.width - highlightWidth) / 2f,
                            y = (size.height - highlightHeight) / 2f + 12,
                        ),
                        size = Size(highlightWidth, highlightHeight),
                    )
                }
            }
        } else {
            Modifier
        },
    )
}
