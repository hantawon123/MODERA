package com.ssafy.modera.feature.onboading.impl

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.onboading.impl.model.OnboardingAnimationState

private const val GREETING_BASE_Y_RATIO = 0.43f
private const val TOP_CAPTION_Y_RATIO = 0.10f
private const val MAIN_TITLE_Y_RATIO = 0.25f
private const val RESULT_TITLE_Y_RATIO = 0.245f
private const val LOTTIE_TOP_Y_RATIO = 0.33f

private const val DEFAULT_ANIMATION_DURATION_MILLIS = 500
private const val GLOW_MOVE_DURATION_MILLIS = 600
private const val TITLE_MOVE_DURATION_MILLIS = 520

private const val CAPTION_SCALE = 0.72f

@Composable
internal fun rememberOnboardingAnimationState(
    phase: OnboardingPhase,
    screenHeight: Dp,
): OnboardingAnimationState {
    val greetingBaseY = screenHeight * GREETING_BASE_Y_RATIO
    val topCaptionY = screenHeight * TOP_CAPTION_Y_RATIO
    val mainTitleY = screenHeight * MAIN_TITLE_Y_RATIO
    val resultTitleY = screenHeight * RESULT_TITLE_Y_RATIO
    val lottieTopY = screenHeight * LOTTIE_TOP_Y_RATIO

    /*
     * Greeting
     */
    val greetingY by animateDpAsState(
        targetValue = when (phase) {
            OnboardingPhase.Greeting -> {
                greetingBaseY
            }

            OnboardingPhase.Intro -> {
                greetingBaseY - 28.dp
            }

            else -> {
                topCaptionY - 60.dp
            }
        },
        animationSpec = onboardingTween(),
        label = "greetingY",
    )

    val greetingAlpha by animateFloatAsState(
        targetValue = when (phase) {
            OnboardingPhase.Greeting,
            OnboardingPhase.Intro
                -> 1f

            else -> 0f
        },
        animationSpec = onboardingTween(),
        label = "greetingAlpha",
    )

    /*
     * Intro description
     */
    val introDescriptionY by animateDpAsState(
        targetValue = when (phase) {
            OnboardingPhase.Greeting -> {
                greetingBaseY + 48.dp
            }

            OnboardingPhase.Intro -> {
                greetingBaseY + 22.dp
            }

            else -> {
                topCaptionY
            }
        },
        animationSpec = onboardingTween(),
        label = "introDescriptionY",
    )

    val introDescriptionAlpha by animateFloatAsState(
        targetValue = when (phase) {
            OnboardingPhase.Intro,
                -> 1f

            else -> 0f
        },
        animationSpec = onboardingTween(),
        label = "introDescriptionAlpha",
    )

    /*
     * Upload title
     */
    val uploadTitleY by animateDpAsState(
        targetValue = when (phase) {
            OnboardingPhase.Greeting,
            OnboardingPhase.Intro,
                -> {
                mainTitleY + 18.dp
            }

            OnboardingPhase.Upload -> {
                mainTitleY
            }

            else -> {
                topCaptionY
            }
        },
        animationSpec = onboardingTween(
            durationMillis = TITLE_MOVE_DURATION_MILLIS,
        ),
        label = "uploadTitleY",
    )

    val uploadTitleAlpha by animateFloatAsState(
        targetValue = when (phase) {
            OnboardingPhase.Upload,
                -> 1f

            else -> 0f
        },
        animationSpec = onboardingTween(),
        label = "uploadTitleAlpha",
    )

    val uploadTitleScale by animateFloatAsState(
        targetValue = if (
            phase == OnboardingPhase.Analysis
        ) {
            CAPTION_SCALE
        } else {
            1f
        },
        animationSpec = onboardingTween(
            durationMillis = TITLE_MOVE_DURATION_MILLIS,
        ),
        label = "uploadTitleScale",
    )

    val uploadTitleColor by animateColorAsState(
        targetValue = if (
            phase == OnboardingPhase.Analysis
        ) {
            ModeraTheme.colors.gray700
        } else {
            ModeraTheme.colors.gray900
        },
        animationSpec = onboardingTween(
            durationMillis = TITLE_MOVE_DURATION_MILLIS,
        ),
        label = "uploadTitleColor",
    )

    /*
     * Analysis title
     */
    val analysisTitleY by animateDpAsState(
        targetValue = when {
            phase.isResultPhase -> {
                topCaptionY
            }

            phase == OnboardingPhase.Analysis -> {
                mainTitleY
            }

            else -> {
                mainTitleY + 18.dp
            }
        },
        animationSpec = onboardingTween(
            durationMillis = TITLE_MOVE_DURATION_MILLIS,
        ),
        label = "analysisTitleY",
    )

    val analysisTitleAlpha by animateFloatAsState(
        targetValue = if (
            phase == OnboardingPhase.Analysis ||
            phase.isResultPhase
        ) {
            1f
        } else {
            0f
        },
        animationSpec = onboardingTween(),
        label = "analysisTitleAlpha",
    )

    val analysisTitleScale by animateFloatAsState(
        targetValue = if (
            phase.isResultPhase
        ) {
            CAPTION_SCALE
        } else {
            1f
        },
        animationSpec = onboardingTween(
            durationMillis = TITLE_MOVE_DURATION_MILLIS,
        ),
        label = "analysisTitleScale",
    )

    val analysisTitleColor by animateColorAsState(
        targetValue = if (
            phase.isResultPhase
        ) {
            ModeraTheme.colors.gray700
        } else {
            ModeraTheme.colors.gray900
        },
        animationSpec = onboardingTween(
            durationMillis = TITLE_MOVE_DURATION_MILLIS,
        ),
        label = "analysisTitleColor",
    )

    /*
     * Glow
     */
    val glowY by animateDpAsState(
        targetValue = when {
            phase == OnboardingPhase.Greeting ||
                    phase == OnboardingPhase.Intro -> {
                greetingBaseY - 130.dp
            }

            phase.isResultPhase -> {
                resultTitleY - 120.dp
            }

            else -> {
                mainTitleY - 130.dp
            }
        },
        animationSpec = onboardingTween(
            durationMillis = GLOW_MOVE_DURATION_MILLIS,
        ),
        label = "glowY",
    )

    return OnboardingAnimationState(
        greetingY = greetingY,
        greetingAlpha = greetingAlpha,
        introDescriptionY = introDescriptionY,
        introDescriptionAlpha = introDescriptionAlpha,
        uploadTitleY = uploadTitleY,
        uploadTitleAlpha = uploadTitleAlpha,
        uploadTitleScale = uploadTitleScale,
        uploadTitleColor = uploadTitleColor,
        analysisTitleY = analysisTitleY,
        analysisTitleAlpha = analysisTitleAlpha,
        analysisTitleScale = analysisTitleScale,
        analysisTitleColor = analysisTitleColor,
        glowY = glowY,
        lottieTopY = lottieTopY,
    )
}

private fun <T> onboardingTween(
    durationMillis: Int = DEFAULT_ANIMATION_DURATION_MILLIS,
) = tween<T>(
    durationMillis = durationMillis,
    easing = FastOutSlowInEasing,
)