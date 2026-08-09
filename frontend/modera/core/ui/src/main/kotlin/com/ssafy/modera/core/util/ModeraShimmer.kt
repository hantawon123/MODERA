package com.ssafy.modera.core.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

private const val ShimmerDurationMillis = 1_200

@Composable
fun Modifier.moderaShimmer(
    shape: Shape = RectangleShape,
): Modifier {
    val baseColor = ModeraTheme.colors.gray100
    val highlightColor = ModeraTheme.colors.gray200

    val transition = rememberInfiniteTransition(
        label = "moderaShimmerTransition",
    )

    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = ShimmerDurationMillis,
                easing = LinearEasing,
            ),
        ),
        label = "moderaShimmerProgress",
    )

    return this
        .clip(shape)
        .drawBehind {
            val startX = size.width * progress

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        baseColor,
                        highlightColor,
                        baseColor,
                    ),
                    start = Offset(
                        x = startX - size.width,
                        y = 0f,
                    ),
                    end = Offset(
                        x = startX,
                        y = size.height,
                    ),
                ),
            )
        }
}