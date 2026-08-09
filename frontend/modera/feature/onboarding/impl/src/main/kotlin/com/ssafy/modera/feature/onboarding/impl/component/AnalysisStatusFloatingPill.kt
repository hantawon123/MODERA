package com.ssafy.modera.feature.onboarding.impl.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.onboarding.impl.R
import com.ssafy.modera.feature.onboarding.impl.model.OnboardingAnalysisState
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun AnalysisStatusFloatingPill(
    analysisState: OnboardingAnalysisState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (analysisState == OnboardingAnalysisState.Idle) {
        return
    }

    val isCompleted = analysisState == OnboardingAnalysisState.Completed

    val width by animateDpAsState(
        targetValue = if (isCompleted) COMPLETED_WIDTH else ANALYZING_WIDTH,
        animationSpec = tween(
            durationMillis = SIZE_ANIMATION_DURATION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "analysisStatusWidth",
    )

    val height by animateDpAsState(
        targetValue = if (isCompleted) COMPLETED_HEIGHT else ANALYZING_HEIGHT,
        animationSpec = tween(
            durationMillis = SIZE_ANIMATION_DURATION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "analysisStatusHeight",
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isCompleted) {
            ModeraTheme.colors.yellow700
        } else {
            ModeraTheme.colors.white
        },
        animationSpec = tween(
            durationMillis = BACKGROUND_ANIMATION_DURATION_MILLIS,
        ),
        label = "analysisStatusBackground",
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isCompleted) COMPLETED_ELEVATION else ANALYZING_ELEVATION,
        animationSpec = tween(
            durationMillis = SIZE_ANIMATION_DURATION_MILLIS,
        ),
        label = "analysisStatusElevation",
    )

    val infiniteTransition = rememberInfiniteTransition(
        label = "analysisBorderTransition",
    )

    val borderProgressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = BORDER_CYCLE_DURATION_MILLIS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "analysisBorderProgress",
    )

    val heartbeatScale = remember {
        Animatable(initialValue = 1f)
    }

    LaunchedEffect(isCompleted) {
        heartbeatScale.snapTo(1f)

        if (!isCompleted) {
            return@LaunchedEffect
        }

        delay(HEARTBEAT_START_DELAY_MILLIS.milliseconds)

        while (true) {
            heartbeatScale.animateTo(
                targetValue = HEARTBEAT_FIRST_SCALE,
                animationSpec = tween(
                    durationMillis = HEARTBEAT_EXPAND_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )

            heartbeatScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = HEARTBEAT_RETURN_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )

            delay(HEARTBEAT_BETWEEN_BEATS_MILLIS.milliseconds)

            heartbeatScale.animateTo(
                targetValue = HEARTBEAT_SECOND_SCALE,
                animationSpec = tween(
                    durationMillis = HEARTBEAT_EXPAND_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )

            heartbeatScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = HEARTBEAT_RETURN_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )

            delay(HEARTBEAT_REST_DURATION_MILLIS.milliseconds)
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = heartbeatScale.value
                scaleY = heartbeatScale.value
            }
            .width(width)
            .height(height)
            .shadow(
                elevation = shadowElevation,
                shape = CircleShape,
                clip = false,
            )
            .background(
                color = backgroundColor,
                shape = CircleShape,
            )
            .clickable(
                enabled = isCompleted,
                onClick = onClick,
            )
            .drawWithCache {
                if (isCompleted) {
                    onDrawWithContent {
                        drawContent()
                    }
                } else {
                    val colors = createAnimatedGradientColors(
                        progress = borderProgressState.value,
                    )

                    val brush = Brush.linearGradient(
                        colors = colors,
                    )

                    val borderWidth = BORDER_WIDTH.toPx()
                    val borderInset = borderWidth / 2f

                    onDrawWithContent {
                        drawContent()

                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(
                                x = borderInset,
                                y = borderInset,
                            ),
                            size = Size(
                                width = size.width - borderWidth,
                                height = size.height - borderWidth,
                            ),
                            cornerRadius = CornerRadius(
                                x = (size.height - borderWidth) / 2f,
                                y = (size.height - borderWidth) / 2f,
                            ),
                            style = Stroke(
                                width = borderWidth,
                            ),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = analysisState,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = TEXT_ENTER_DURATION_MILLIS,
                        delayMillis = TEXT_ENTER_DELAY_MILLIS,
                    ),
                ) togetherWith fadeOut(
                    animationSpec = tween(
                        durationMillis = TEXT_EXIT_DURATION_MILLIS,
                    ),
                )
            },
            label = "analysisStatusText",
        ) { state ->
            val textResource = when (state) {
                OnboardingAnalysisState.Idle,
                OnboardingAnalysisState.Analyzing,
                    -> R.string.onboarding_analysis_analyzing

                OnboardingAnalysisState.Completed ->
                    R.string.onboarding_analysis_completed
            }

            Text(
                text = stringResource(textResource),
                modifier = Modifier.padding(
                    horizontal = TEXT_HORIZONTAL_PADDING,
                ),
                style = ModeraTheme.typography.bodySB16,
                color = if (state == OnboardingAnalysisState.Completed) {
                    ModeraTheme.colors.white
                } else {
                    ModeraTheme.colors.gray700
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun createAnimatedGradientColors(
    progress: Float,
): List<Color> {
    val colors = AI_BORDER_COLORS
    if (colors.isEmpty()) {
        return emptyList()
    }

    val colorCount = colors.size
    val position = progress * colorCount
    val floorPosition = floor(position)
    val baseIndex = floorPosition.toInt()
    val fraction = position - floorPosition

    return List(
        size = colorCount + 1,
    ) { index ->
        val currentIndex = (index + baseIndex) % colorCount
        val nextIndex = (currentIndex + 1) % colorCount

        lerp(
            start = colors[currentIndex],
            stop = colors[nextIndex],
            fraction = fraction,
        )
    }
}

private val AI_BORDER_COLORS = listOf(
    Color(0xFFFFFF00),
    Color(0xFFFFC700),
    Color(0xFFFF8200),
    Color(0xFFFFEA00),
    Color(0xFFFF5A1F)
)

private val ANALYZING_WIDTH = 100.dp
private val COMPLETED_WIDTH = 272.dp

private val ANALYZING_HEIGHT = 40.dp
private val COMPLETED_HEIGHT = 56.dp

private val TEXT_HORIZONTAL_PADDING = 12.dp

private val ANALYZING_ELEVATION = 4.dp
private val COMPLETED_ELEVATION = 0.dp

private val BORDER_WIDTH = 1.5.dp

private const val BORDER_CYCLE_DURATION_MILLIS = 2_800

private const val SIZE_ANIMATION_DURATION_MILLIS = 480
private const val BACKGROUND_ANIMATION_DURATION_MILLIS = 300

private const val TEXT_ENTER_DURATION_MILLIS = 240
private const val TEXT_ENTER_DELAY_MILLIS = 100
private const val TEXT_EXIT_DURATION_MILLIS = 120

private const val HEARTBEAT_START_DELAY_MILLIS = 650L

private const val HEARTBEAT_FIRST_SCALE = 1.09f
private const val HEARTBEAT_SECOND_SCALE = 1.08f

private const val HEARTBEAT_EXPAND_DURATION_MILLIS = 110
private const val HEARTBEAT_RETURN_DURATION_MILLIS = 140

private const val HEARTBEAT_BETWEEN_BEATS_MILLIS = 80L
private const val HEARTBEAT_REST_DURATION_MILLIS = 1_600L