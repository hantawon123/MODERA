package com.ssafy.modera.feature.home.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.home.R

@Composable
internal fun SearchAnalyzingScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        SearchAnalyzingGradientText(
            text = stringResource(R.string.home_search_analyzing),
        )
    }
}

@Composable
private fun SearchAnalyzingGradientText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = ModeraTheme.colors
    val gradientColors = listOf(
        colors.yellow500,
        colors.yellow700,
        colors.yellow700,
        colors.yellow500,
    )

    var textWidthPx by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "searchAnalyzingText")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SearchAnalyzingDefaults.GradientAnimationDurationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "searchAnalyzingGradientShift",
    )

    val travelRange = (textWidthPx * SearchAnalyzingDefaults.GradientTravelRatio).coerceAtLeast(1f)

    BasicText(
        text = text,
        modifier = modifier,
        onTextLayout = { textLayoutResult ->
            textWidthPx = textLayoutResult.size.width.toFloat()
        },
        style = ModeraTheme.typography.bodySB16.copy(
            brush = Brush.linearGradient(
                colors = gradientColors,
                start = Offset(-travelRange + gradientShift * travelRange * 2f, 0f),
                end = Offset(textWidthPx + travelRange - gradientShift * travelRange * 2f, 0f),
            ),
        ),
    )
}

private object SearchAnalyzingDefaults {
    const val GradientAnimationDurationMillis = 1800
    const val GradientTravelRatio = 0.6f
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun SearchAnalyzingScreenPreview() {
    ModeraTheme {
        SearchAnalyzingScreen()
    }
}
