package com.ssafy.modera.feature.analyzedimage.detail.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
internal fun KeyInformationSection(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        HighlightedTitle(text = title)

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { item ->
                KeyInformationItem(
                    text = item,
                )
            }
        }
    }
}

@Composable
private fun HighlightedTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val highlightColor =
        ModeraTheme.colors.yellow500.copy(alpha = 0.35f)

    val highlightProgress = remember {
        Animatable(0f)
    }

    LaunchedEffect(text) {
        highlightProgress.snapTo(0f)

        highlightProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 560,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Text(
        text = text,
        style = ModeraTheme.typography.bodySB16,
        color = ModeraTheme.colors.gray900,
        modifier = modifier.drawBehind {
            val horizontalPadding = 4.dp.toPx()
            val highlightHeight = 10.dp.toPx()

            val fullWidth =
                size.width + horizontalPadding * 2

            val highlightWidth =
                fullWidth * highlightProgress.value

            val topLeft = Offset(
                x = -horizontalPadding,
                y = size.height - highlightHeight + 2.dp.toPx(),
            )

            rotate(
                degrees = -3f,
                pivot = Offset(
                    x = -horizontalPadding,
                    y = size.height - highlightHeight / 2,
                ),
            ) {
                drawRoundRect(
                    color = highlightColor,
                    topLeft = topLeft,
                    size = Size(
                        width = highlightWidth,
                        height = highlightHeight,
                    ),
                    cornerRadius = CornerRadius(
                        x = 2.dp.toPx(),
                        y = 2.dp.toPx(),
                    ),
                )
            }
        },
    )
}

@Composable
private fun KeyInformationItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colonIndex = text.indexOf(':')

    val annotatedText = buildAnnotatedString {
        withStyle(
            style = ModeraTheme.typography.bodySB16.copy(color = ModeraTheme.colors.gray900)
                .toSpanStyle(),
        ) {
            append(text.substring(startIndex = 0, endIndex = colonIndex))
        }

        append(text.substring(colonIndex))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = annotatedText,
            modifier = Modifier.weight(1f),
            style = ModeraTheme.typography.bodyR16,
            color = ModeraTheme.colors.gray700,
        )
    }
}

@Preview(
    name = "KeyInformationSection",
    showBackground = true,
)
@Composable
private fun KeyInformationSectionPreview() {
    ModeraTheme {
        KeyInformationSection(
            title = "핵심 정보",
            items = listOf(
                "교육 기관: SSAFY",
                "교육 목표: 진짜 프로그램을 잘 짜는 인력 양성",
                "핵심 역량 1: 본인이 직접 프로그램을 작성하는 능력 (코딩 테스트로 검증)",
                "핵심 역량 2: 정확성과 성능을 생각할 수 있는 역량",
            ),
            modifier = Modifier.padding(24.dp),
        )
    }
}