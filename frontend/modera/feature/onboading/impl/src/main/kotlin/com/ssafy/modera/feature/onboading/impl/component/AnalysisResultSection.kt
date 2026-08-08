package com.ssafy.modera.feature.onboading.impl.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.onboading.impl.OnboardingPhase
import com.ssafy.modera.feature.onboading.impl.R
import com.ssafy.modera.feature.onboading.impl.isResultPhase
import com.ssafy.modera.feature.onboading.impl.model.HighlightRect

@Composable
internal fun BoxWithConstraintsScope.AnalysisResultSection(
    phase: OnboardingPhase,
) {
    val sectionTopY =
        maxHeight * RESULT_SECTION_TOP_Y_RATIO

    val previewY =
        maxHeight * RESULT_PREVIEW_Y_RATIO

    val previewOffsetY =
        previewY - sectionTopY

    val sectionHeight =
        maxHeight - sectionTopY

    AnimatedVisibility(
        visible = phase.isResultPhase,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(
                y = sectionTopY,
            )
            .fillMaxWidth()
            .height(sectionHeight),
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = RESULT_SECTION_ENTER_DURATION_MILLIS,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = RESULT_SECTION_ENTER_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetY = {
                RESULT_SECTION_ENTER_OFFSET_PX
            },
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            AnalysisFeatureTitle(
                phase = phase,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                    ),
            )

            AnalysisResultPreview(
                phase = phase,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = previewOffsetY,
                    ),
            )
        }
    }
}

@Composable
private fun AnalysisResultPreview(
    phase: OnboardingPhase,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val targetRect =
        phase.highlightRect

    val targetScrollY =
        phase.targetScrollY(
            targetRect = targetRect,
        )

    LaunchedEffect(
        phase,
        targetScrollY,
    ) {
        val targetScrollPx = with(density) {
            targetScrollY.roundToPx()
        }

        scrollState.animateScrollTo(
            value = targetScrollPx,
            animationSpec = tween(
                durationMillis = RESULT_SCROLL_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Box(
        modifier = modifier
            .size(
                width = ANALYSIS_IMAGE_WIDTH,
                height = RESULT_VIEWPORT_HEIGHT,
            )
            .clipToBounds()
            .verticalScroll(
                state = scrollState,
                enabled = false,
            ),
    ) {
        Box(
            modifier = Modifier.size(
                width = ANALYSIS_IMAGE_WIDTH,
                height = ANALYSIS_SCROLL_CONTENT_HEIGHT,
            ),
        ) {
            Image(
                painter = painterResource(
                    R.drawable.onboarding_analysis_result,
                ),
                contentDescription = null,
                modifier = Modifier.size(
                    width = ANALYSIS_IMAGE_WIDTH,
                    height = ANALYSIS_IMAGE_HEIGHT,
                ),
                contentScale = ContentScale.FillBounds,
            )

            Box(
                modifier = Modifier
                    .offset(
                        x = targetRect.left,
                        y = targetRect.top,
                    )
                    .size(
                        width = targetRect.width,
                        height = targetRect.height,
                    )
                    .background(
                        color = ModeraTheme.colors.yellow500.copy(
                            alpha = HIGHLIGHT_BACKGROUND_ALPHA,
                        ),
                        shape = RoundedCornerShape(
                            HIGHLIGHT_CORNER_RADIUS,
                        ),
                    )
                    .border(
                        width = HIGHLIGHT_BORDER_WIDTH,
                        color = ModeraTheme.colors.yellow500,
                        shape = RoundedCornerShape(
                            HIGHLIGHT_CORNER_RADIUS,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun AnalysisFeatureTitle(
    phase: OnboardingPhase,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = phase,
        modifier = modifier
            .height(FEATURE_TITLE_HEIGHT),
        transitionSpec = {
            (
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = FEATURE_TEXT_ENTER_DURATION_MILLIS,
                            delayMillis = FEATURE_TEXT_ENTER_DELAY_MILLIS,
                        ),
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = FEATURE_TEXT_ENTER_DURATION_MILLIS,
                            delayMillis = FEATURE_TEXT_ENTER_DELAY_MILLIS,
                            easing = FastOutSlowInEasing,
                        ),
                        initialOffsetY = { height ->
                            height / 2
                        },
                    )
                    ) togetherWith (
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = FEATURE_TEXT_EXIT_DURATION_MILLIS,
                        ),
                    ) + slideOutVertically(
                        animationSpec = tween(
                            durationMillis = FEATURE_TEXT_EXIT_DURATION_MILLIS,
                            easing = FastOutSlowInEasing,
                        ),
                        targetOffsetY = { height ->
                            -height / 2
                        },
                    )
                    )
        },
        label = "analysisFeatureTitle",
    ) { targetPhase ->
        val text = when (targetPhase) {
            OnboardingPhase.CategoryHighlight -> {
                stringResource(
                    R.string.onboarding_analysis_category,
                )
            }

            OnboardingPhase.HashtagHighlight -> {
                stringResource(
                    R.string.onboarding_analysis_hashtag,
                )
            }

            OnboardingPhase.SummaryHighlight -> {
                stringResource(
                    R.string.onboarding_analysis_summary,
                )
            }

            OnboardingPhase.RelatedHighlight -> {
                stringResource(
                    R.string.onboarding_analysis_related,
                )
            }

            else -> {
                ""
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = ModeraTheme.typography.titleB22,
                color = ModeraTheme.colors.gray900,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val OnboardingPhase.highlightRect: HighlightRect
    get() = when (this) {
        OnboardingPhase.CategoryHighlight -> {
            HighlightRect(
                left = 18.dp,
                top = 50.dp,
                width = 58.dp,
                height = 36.dp,
            )
        }

        OnboardingPhase.HashtagHighlight -> {
            HighlightRect(
                left = 18.dp,
                top = 160.dp,
                width = 312.dp,
                height = 56.dp,
            )
        }

        OnboardingPhase.SummaryHighlight -> {
            HighlightRect(
                left = 18.dp,
                top = 212.dp,
                width = 312.dp,
                height = 128.dp,
            )
        }

        OnboardingPhase.RelatedHighlight -> {
            HighlightRect(
                left = 18.dp,
                top = 888.dp,
                width = 312.dp,
                height = 58.dp,
            )
        }

        else -> {
            HighlightRect(
                left = 18.dp,
                top = 50.dp,
                width = 58.dp,
                height = 36.dp,
            )
        }
    }

private fun OnboardingPhase.targetScrollY(
    targetRect: HighlightRect,
) = when (this) {
    OnboardingPhase.CategoryHighlight -> {
        0.dp
    }

    else -> {
        (targetRect.top - HIGHLIGHT_FOCUS_TOP)
            .coerceIn(
                minimumValue = 0.dp,
                maximumValue = MAX_RESULT_SCROLL_Y,
            )
    }
}

private const val RESULT_SECTION_TOP_Y_RATIO = 0.245f
private const val RESULT_PREVIEW_Y_RATIO = 0.48f

private const val RESULT_SECTION_ENTER_DURATION_MILLIS = 500
private const val RESULT_SECTION_ENTER_OFFSET_PX = 36

private const val FEATURE_TEXT_ENTER_DURATION_MILLIS = 420
private const val FEATURE_TEXT_ENTER_DELAY_MILLIS = 80
private const val FEATURE_TEXT_EXIT_DURATION_MILLIS = 280

private val FEATURE_TITLE_HEIGHT = 56.dp

private val ANALYSIS_IMAGE_WIDTH = 344.dp
private val ANALYSIS_IMAGE_HEIGHT = 950.dp

private val ANALYSIS_BOTTOM_SCROLL_SPACE = 240.dp

private val ANALYSIS_SCROLL_CONTENT_HEIGHT =
    ANALYSIS_IMAGE_HEIGHT +
            ANALYSIS_BOTTOM_SCROLL_SPACE

private val RESULT_VIEWPORT_HEIGHT = 440.dp

private val MAX_RESULT_SCROLL_Y =
    ANALYSIS_SCROLL_CONTENT_HEIGHT -
            RESULT_VIEWPORT_HEIGHT

private val HIGHLIGHT_FOCUS_TOP = 96.dp

private const val RESULT_SCROLL_DURATION_MILLIS = 720

private const val HIGHLIGHT_BACKGROUND_ALPHA = 0.12f

private val HIGHLIGHT_CORNER_RADIUS = 8.dp
private val HIGHLIGHT_BORDER_WIDTH = 2.dp