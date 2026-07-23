package com.ssafy.modera.feature.home.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.IconButton
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.home.R

private val BannerBackground = Color(0xFFF4F7FF)
private val BannerBorder = Color(0xFFDCE6FF)
private val StatusBlue = Color(0xFF2563EB)
private val ProgressGradientStart = Color(0xFF3B5CFF)
private val ProgressGradientEnd = Color(0xFF8E44FF)
private val ProgressGradientColors = listOf(
    ProgressGradientStart,
    ProgressGradientEnd,
    ProgressGradientStart,
    ProgressGradientEnd,
)

@Composable
internal fun AiAnalysisProgressBanner(
    imageCount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "analysisBanner")
    val dotPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dots",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BannerBackground)
            .border(
                width = 1.dp,
                color = BannerBorder,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8EEFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ai_sparkle),
                    contentDescription = null,
                    tint = StatusBlue,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_analysis_status),
                        style = ModeraTheme.typography.body2Medium,
                        color = StatusBlue,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    LoadingDots(activeDotCount = (dotPhase.toInt() % 3) + 1)
                }

                Text(
                    text = stringResource(R.string.home_analysis_message, imageCount),
                    style = ModeraTheme.typography.subtitle3SemiBold,
                    color = ModeraTheme.colors.typo,
                )
            }

            IconButton(
                onClick = onDismiss,
                painter = painterResource(R.drawable.ic_close),
                modifier = Modifier.size(24.dp),
                contentDescription = stringResource(R.string.home_analysis_dismiss_content_description),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedShimmerProgressBar()
    }
}

@Composable
private fun AnimatedShimmerProgressBar(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "progressGradient")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gradientShift",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp)),
    ) {
        val barWidth = constraints.maxWidth.toFloat()
        val travelRange = barWidth * 0.6f

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = ProgressGradientColors,
                        start = Offset(-travelRange + gradientShift * travelRange * 2f, 0f),
                        end = Offset(barWidth + travelRange - gradientShift * travelRange * 2f, 0f),
                    ),
                ),
        )
    }
}

@Composable
private fun LoadingDots(
    activeDotCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (index < activeDotCount) StatusBlue else StatusBlue.copy(alpha = 0.25f),
                    ),
            )
        }
    }
}

@Preview(
    name = "AI Analysis Progress Banner",
    showBackground = true,
)
@Composable
private fun AiAnalysisProgressBannerPreview() {
    ModeraTheme {
        AiAnalysisProgressBanner(
            imageCount = 17,
            onDismiss = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}
