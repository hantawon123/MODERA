package com.ssafy.modera.core.component.item

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

enum class SearchBarMode {
    General,
    Ai,
}

/**
 * 검색바 — placeholder / 검색어 입력, [SearchBarMode]에 따라 강조 색상이 달라진다.
 *
 * - [SearchBarMode.General]: gray500 테두리·아이콘
 * - [SearchBarMode.Ai]: yellow700 테두리, 검색 아이콘은 노랑→주황→빨강 색상 애니메이션
 */
@Composable
fun ModeraSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    mode: SearchBarMode,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val colors = ModeraTheme.colors
    val accentColor = when (mode) {
        SearchBarMode.General -> colors.gray500
        SearchBarMode.Ai -> colors.yellow700
    }
    val shape = SearchBarDefaults.Shape
    val resolvedFocusRequester = focusRequester ?: remember { FocusRequester() }
    val trailingIconsWidth =
        SearchBarDefaults.IconSize * 2 + SearchBarDefaults.IconTextSpacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color = colors.white, shape = shape)
            .border(
                width = SearchBarDefaults.BorderWidth,
                color = accentColor,
                shape = shape,
            )
            .padding(
                horizontal = SearchBarDefaults.HorizontalPadding,
                vertical = SearchBarDefaults.VerticalPadding,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .focusRequester(resolvedFocusRequester)
                .fillMaxWidth()
                .padding(end = trailingIconsWidth)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            enabled = enabled,
            singleLine = true,
            textStyle = ModeraTheme.typography.bodyR16.copy(color = colors.gray900),
            cursorBrush = SolidColor(accentColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { onSearch?.invoke() },
            ),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = ModeraTheme.typography.bodyR16,
                            color = colors.gray400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(trailingIconsWidth),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = ImageVector.vectorResource(ModeraIcons.CloseCircle),
                    contentDescription = "초기화",
                    modifier = Modifier
                        .size(SearchBarDefaults.IconSize)
                        .clickable(
                            enabled = enabled,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onQueryChange("") },
                        ),
                    tint = colors.gray500,
                )

                Spacer(Modifier.width(SearchBarDefaults.IconTextSpacing))
            }

            SearchIcon(
                mode = mode,
                accentColor = accentColor,
                onClick = onSearch,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun SearchIcon(
    mode: SearchBarMode,
    accentColor: Color,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val iconTint = when (mode) {
        SearchBarMode.General -> accentColor
        SearchBarMode.Ai -> rememberAiSearchIconColor()
    }
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            enabled = enabled,
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Icon(
        imageVector = ImageVector.vectorResource(ModeraIcons.Search),
        contentDescription = "검색",
        modifier = modifier
            .size(SearchBarDefaults.IconSize)
            .then(clickableModifier),
        tint = iconTint,
    )
}

@Composable
private fun rememberAiSearchIconColor(): Color {
    val yellow = ModeraTheme.colors.yellow500
    val orange = ModeraTheme.colors.yellow700
    val red = ModeraTheme.colors.red

    val infiniteTransition = rememberInfiniteTransition(label = "aiSearchIcon")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SearchBarDefaults.AiIconAnimationDurationMs,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aiSearchIconProgress",
    )

    return when {
        progress <= 1f -> lerp(yellow, orange, progress)
        else -> lerp(orange, red, progress - 1f)
    }
}

object SearchBarDefaults {
    val Shape = RoundedCornerShape(12.dp)
    val BorderWidth = 1.dp
    val HorizontalPadding = 16.dp
    val VerticalPadding = 14.dp
    val IconSize = 24.dp
    val IconTextSpacing = 8.dp
    const val AiIconAnimationDurationMs = 1800
}

@Composable
private fun SearchBarPreviewHost(content: @Composable () -> Unit) {
    ModeraTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ModeraTheme.colors.gray100)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            content()
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFF5F5F3,
    widthDp = 360,
    heightDp = 200,
)
@Composable
private fun ModeraSearchBarGeneralPlaceholderPreview() {
    SearchBarPreviewHost {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeraSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "가장 최근에 저장한 레시피",
                mode = SearchBarMode.General,
            )

            ModeraSearchBar(
                query = "검색어를 입력 중일 땐 이렇게",
                onQueryChange = {},
                placeholder = "가장 최근에 저장한 레시피",
                mode = SearchBarMode.General,
            )
        }
    }
}

@Preview(
    name = "AI SearchBar (animation)",
    showBackground = true,
    backgroundColor = 0xFFF5F5F3,
    widthDp = 360,
    heightDp = 200,
)
@Composable
private fun ModeraSearchBarAiPreview() {
    SearchBarPreviewHost {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeraSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "AI에게 물어보기",
                mode = SearchBarMode.Ai,
            )
            ModeraSearchBar(
                query = "검색어를 입력 중일 땐 이렇게 검색어를 입력 중일 땐 이렇게",
                onQueryChange = {},
                placeholder = "AI에게 물어보기",
                mode = SearchBarMode.Ai,
            )
        }
    }
}