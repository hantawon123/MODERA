package com.ssafy.modera.core.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

/**
 * 해시태그 목록을 Row로 표시한다.
 *
 * @param tags 해시태그 단어 리스트
 * @param modifier Row modifier
 * @param color 텍스트 색상 (기본 blue)
 * @param style 텍스트 스타일 (기본 bodyR14)
 * @param onTagClick 태그 클릭 콜백 (null이면 클릭 불가)
 */
@Composable
fun ModeraHashtags(
    tags: List<String>,
    modifier: Modifier = Modifier,
    color: Color = ModeraTheme.colors.blue,
    style: TextStyle = ModeraTheme.typography.bodyR14,
    onTagClick: ((String) -> Unit)? = null,
) {
    if (tags.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        tags.forEach { tag ->
            Text(
                text = "#$tag",
                style = style,
                color = color,
                modifier = if (onTagClick != null) {
                    Modifier.clickable {
                        onTagClick(tag)
                    }
                } else {
                    Modifier
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModeraHashtagsPreview() {
    ModeraTheme {
        ModeraHashtags(
            tags = listOf("해커톤", "SW", "해커톤", "SW"),
        )
    }
}
