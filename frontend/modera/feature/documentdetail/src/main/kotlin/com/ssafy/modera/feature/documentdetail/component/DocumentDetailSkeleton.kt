package com.ssafy.modera.feature.documentdetail.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.util.moderaShimmer
import com.ssafy.modera.feature.documentdetail.R
import kotlin.math.PI
import kotlin.math.sin

private const val ReanalyzingTextDurationMillis = 1_400

private const val CharacterDelayFraction = 0.08f
private const val CharacterBounceFraction = 0.22f

private val CharacterBounceHeight = 5.dp

@Composable
internal fun DocumentDetailSkeleton(
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
                .padding(bottom = 40.dp),
        ) {
            DocumentHeaderSkeleton()

            Spacer(modifier = Modifier.height(28.dp))

            SummarySkeleton()

            Spacer(modifier = Modifier.height(28.dp))

            DocumentContentSkeleton()
        }

        ReanalyzingText(
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun DocumentHeaderSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .height(24.dp),
            cornerRadius = 6.dp,
        )

        Spacer(modifier = Modifier.height(6.dp))

        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .height(24.dp),
            cornerRadius = 6.dp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        DocumentInfoSkeleton()

        Spacer(modifier = Modifier.height(16.dp))

        ManageImagesSkeleton()
    }
}

@Composable
private fun DocumentInfoSkeleton() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBox(
            modifier = Modifier
                .width(72.dp)
                .height(16.dp),
            cornerRadius = 4.dp,
        )

        SkeletonBox(
            modifier = Modifier
                .width(116.dp)
                .height(16.dp),
            cornerRadius = 4.dp,
        )
    }
}

@Composable
private fun ManageImagesSkeleton() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBox(
            modifier = Modifier
                .width(128.dp)
                .height(34.dp),
            cornerRadius = 8.dp,
        )

        SkeletonBox(
            modifier = Modifier.size(16.dp),
            shape = CircleShape,
        )
    }
}

@Composable
private fun SummarySkeleton() {
    SkeletonBox(
        modifier = Modifier
            .width(42.dp)
            .height(20.dp),
        cornerRadius = 4.dp,
    )

    Spacer(modifier = Modifier.height(8.dp))

    TextLinesSkeleton(
        lineWidths = listOf(
            1f,
            0.94f,
            0.68f,
        ),
    )
}

@Composable
private fun DocumentContentSkeleton() {
    SkeletonBox(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .height(24.dp),
        cornerRadius = 5.dp,
    )

    Spacer(modifier = Modifier.height(14.dp))

    TextLinesSkeleton(
        lineWidths = listOf(
            1f,
            1f,
            0.92f,
            0.76f,
        ),
    )

    Spacer(modifier = Modifier.height(28.dp))

    SkeletonBox(
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .height(22.dp),
        cornerRadius = 5.dp,
    )

    Spacer(modifier = Modifier.height(14.dp))

    TextLinesSkeleton(
        lineWidths = listOf(
            1f,
            0.96f,
            1f,
            0.82f,
            0.58f,
        ),
    )

    Spacer(modifier = Modifier.height(28.dp))

    SkeletonBox(
        modifier = Modifier
            .fillMaxWidth(0.48f)
            .height(22.dp),
        cornerRadius = 5.dp,
    )

    Spacer(modifier = Modifier.height(14.dp))

    SkeletonBox(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        cornerRadius = 8.dp,
    )
}

@Composable
private fun TextLinesSkeleton(
    lineWidths: List<Float>,
) {
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
private fun ReanalyzingText(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(
        label = "documentReanalyzingTextTransition",
    )

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = ReanalyzingTextDurationMillis,
                easing = LinearEasing,
            ),
        ),
        label = "documentReanalyzingTextProgress",
    )

    val bounceHeightPx = with(LocalDensity.current) {
        CharacterBounceHeight.toPx()
    }

    val text = stringResource(
        R.string.document_detail_reanalyzing,
    )

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
                                        bounceProgress * PI,
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

@Preview(name = "Document Detail Skeleton", showBackground = true)
@Composable
private fun DocumentDetailSkeletonPreview() {
    ModeraTheme {
        DocumentDetailSkeleton(
            scrollState = rememberScrollState(),
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets
                        .safeDrawing
                        .only(WindowInsetsSides.Vertical)
                )
                .background(ModeraTheme.colors.white)
                .padding(horizontal = 24.dp),
        )
    }
}