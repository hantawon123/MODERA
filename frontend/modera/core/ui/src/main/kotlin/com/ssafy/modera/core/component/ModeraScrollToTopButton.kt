package com.ssafy.modera.core.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.ui.R

/**
 * 리스트를 맨 위로 스크롤하는 플로팅 버튼.
 *
 * @param visible 버튼 표시 여부
 * @param onClick 버튼 클릭 시 실행할 콜백. 보통 `listState.animateScrollToItem(0)`을 호출한다.
 * @param modifier 버튼 레이아웃 modifier
 */
@Composable
fun ModeraScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
    ) {
        Box(
            modifier = Modifier
                .padding(ScrollToTopDefaults.EdgePadding)
                .size(ScrollToTopDefaults.ButtonSize)
                .clip(CircleShape)
                .background(ModeraTheme.colors.gray900.copy(alpha = ScrollToTopDefaults.BackgroundAlpha))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(ModeraIcons.ArrowUp),
                contentDescription = stringResource(R.string.scroll_to_top_description),
                tint = ModeraTheme.colors.white,
                modifier = Modifier.size(ScrollToTopDefaults.IconSize),
            )
        }
    }
}

/**
 * [LazyListState]의 스크롤 위치에 따라 맨 위로 버튼 표시 여부를 계산한다.
 *
 * @param listState 관찰할 LazyColumn/LazyRow의 스크롤 상태
 * @param visibilityThresholdPx 첫 아이템이 아직 보일 때, 이 픽셀 이상 스크롤되면 버튼을 표시한다.
 * @return 맨 위로 버튼을 표시해야 하면 true
 */
@Composable
fun rememberShowScrollToTop(
    listState: LazyListState,
    visibilityThresholdPx: Int = ScrollToTopDefaults.VisibilityThresholdPx,
): Boolean {
    val showScrollToTop by remember(listState, visibilityThresholdPx) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > visibilityThresholdPx
        }
    }
    return showScrollToTop
}

object ScrollToTopDefaults {
    val ButtonSize = 40.dp
    val IconSize = 24.dp
    val EdgePadding = 16.dp
    const val BackgroundAlpha = 0.72f
    const val VisibilityThresholdPx = 200
}

@Preview(showBackground = true)
@Composable
private fun ModeraScrollToTopButtonPreview() {
    ModeraTheme {
        ModeraScrollToTopButton(
            visible = true,
            onClick = {},
        )
    }
}
