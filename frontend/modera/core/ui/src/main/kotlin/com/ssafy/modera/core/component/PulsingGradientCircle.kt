package com.ssafy.modera.core.component

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

private const val PulseScaleMin = 0.8f
private const val PulseScaleMax = 1.1f

@Composable
fun PulsingGradientCircle(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")

    val scale by infiniteTransition.animateFloat(
        initialValue = PulseScaleMin,
        targetValue = PulseScaleMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    val pulseProgress = ((scale - PulseScaleMin) / (PulseScaleMax - PulseScaleMin))
        .coerceIn(0f, 1f)
    val pulseColor = lerp(
        start = ModeraTheme.colors.yellow600,
        stop = ModeraTheme.colors.yellow500,
        fraction = pulseProgress,
    )

    val gradientBrush = Brush.radialGradient(
        colors = listOf(
            pulseColor.copy(alpha = 0.6f),
            pulseColor.copy(alpha = 0.0f),
        ),
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(1f)
                .scale(scale)
                .background(
                    brush = gradientBrush,
                    shape = CircleShape,
                ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 360)
@Composable
private fun PulsingGradientCirclePreview() {
    ModeraTheme {
        PulsingGradientCircle()
    }
}
