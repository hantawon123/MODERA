package com.ssafy.modera.feature.analyzedimagedetail.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.util.moderaShimmer
import kotlin.math.PI
import kotlin.math.sin

private const val AnalyzingTextDurationMillis = 1_400

private const val CharacterDelayFraction = 0.08f
private const val CharacterBounceFraction = 0.22f

private val CharacterBounceHeight = 5.dp

@Composable
internal fun AnalyzedImageDetailSkeleton(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    start = 24.dp,
                    top = 8.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
        ) {
            HeaderSkeleton()

            Spacer(modifier = Modifier.height(16.dp))

            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(26.dp),
                cornerRadius = 6.dp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            SkeletonBox(
                modifier = Modifier
                    .width(132.dp)
                    .height(16.dp),
                cornerRadius = 4.dp,
            )

            Spacer(modifier = Modifier.height(10.dp))

            ActionItemsSkeleton()

            Spacer(modifier = Modifier.height(24.dp))

            HashtagsSkeleton()

            Spacer(modifier = Modifier.height(30.dp))

            TextSectionSkeleton(
                titleWidth = 76.dp,
                lineWidths = listOf(
                    1f,
                    1f,
                    0.82f,
                ),
            )

            Spacer(modifier = Modifier.height(30.dp))

            TextSectionSkeleton(
                titleWidth = 92.dp,
                lineWidths = listOf(
                    1f,
                    0.94f,
                    1f,
                    0.68f,
                ),
            )

            Spacer(modifier = Modifier.height(30.dp))

            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                cornerRadius = 12.dp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                cornerRadius = 12.dp,
            )
        }

        AnalyzingText(
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun AnalyzingText(
    modifier: Modifier = Modifier,
    text: String = "분석중...",
) {
    val transition = rememberInfiniteTransition(
        label = "analyzingTextTransition",
    )

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnalyzingTextDurationMillis,
                easing = LinearEasing,
            ),
        ),
        label = "analyzingTextProgress",
    )

    val bounceHeightPx = with(LocalDensity.current) {
        CharacterBounceHeight.toPx()
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        text.forEachIndexed { index, character ->
            if (character == ' ') {
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Text(
                    text = character.toString(),
                    style = ModeraTheme.typography.bodySB16,
                    color = ModeraTheme.colors.yellow700,
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .graphicsLayer {
                            val delayedProgress = (
                                    progress -
                                            index * CharacterDelayFraction +
                                            1f
                                    ) % 1f

                            val bounceProgress =
                                if (
                                    delayedProgress <=
                                    CharacterBounceFraction
                                ) {
                                    delayedProgress /
                                            CharacterBounceFraction
                                } else {
                                    0f
                                }

                            val bounceOffset =
                                if (bounceProgress > 0f) {
                                    sin(
                                        bounceProgress *
                                                PI,
                                    ).toFloat()
                                } else {
                                    0f
                                }

                            translationY =
                                -bounceOffset * bounceHeightPx
                        },
                )
            }
        }
    }
}

@Composable
private fun HeaderSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SkeletonBox(
            modifier = Modifier
                .width(58.dp)
                .height(28.dp),
            cornerRadius = 8.dp,
        )

        SkeletonBox(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
        )
    }
}

@Composable
private fun ActionItemsSkeleton() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SkeletonBox(
            modifier = Modifier
                .width(82.dp)
                .height(32.dp),
            cornerRadius = 8.dp,
        )

        SkeletonBox(
            modifier = Modifier
                .width(82.dp)
                .height(32.dp),
            cornerRadius = 8.dp,
        )
    }
}

@Composable
private fun HashtagsSkeleton() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SkeletonBox(
            modifier = Modifier
                .width(64.dp)
                .height(26.dp),
            cornerRadius = 13.dp,
        )

        SkeletonBox(
            modifier = Modifier
                .width(76.dp)
                .height(26.dp),
            cornerRadius = 13.dp,
        )

        SkeletonBox(
            modifier = Modifier
                .width(58.dp)
                .height(26.dp),
            cornerRadius = 13.dp,
        )
    }
}

@Composable
private fun TextSectionSkeleton(
    titleWidth: Dp,
    lineWidths: List<Float>,
) {
    SkeletonBox(
        modifier = Modifier
            .width(titleWidth)
            .height(20.dp),
        cornerRadius = 4.dp,
    )

    Spacer(modifier = Modifier.height(14.dp))

    lineWidths.forEachIndexed { index, widthFraction ->
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(16.dp),
            cornerRadius = 4.dp,
        )

        if (index != lineWidths.lastIndex) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
) {
    Box(
        modifier = modifier.moderaShimmer(
            shape = shape,
        ),
    )
}

@Preview(
    name = "AnalyzedImageDetailSkeleton",
    showBackground = true,
)
@Composable
private fun AnalyzedImageDetailSkeletonPreview() {
    ModeraTheme {
        AnalyzedImageDetailSkeleton(
            scrollState = rememberScrollState(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}