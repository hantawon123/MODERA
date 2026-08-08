package com.ssafy.modera.feature.onboading.impl.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.ssafy.modera.core.component.PulsingGradientCircle
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.onboading.impl.OnboardingPhase
import com.ssafy.modera.feature.onboading.impl.OnboardingPhase.Analysis
import com.ssafy.modera.feature.onboading.impl.OnboardingPhase.Greeting
import com.ssafy.modera.feature.onboading.impl.OnboardingPhase.Intro
import com.ssafy.modera.feature.onboading.impl.OnboardingPhase.Upload
import com.ssafy.modera.feature.onboading.impl.R
import com.ssafy.modera.feature.onboading.impl.isResultPhase

@Composable
internal fun BoxWithConstraintsScope.OnboardingGuideSection(
    phase: OnboardingPhase,
    lottieProgress: Float,
    lottieComposition: LottieComposition?,
) {
    val screenHeight = maxHeight

    val greetingBaseY =
        screenHeight * GREETING_BASE_Y_RATIO

    val topCaptionY =
        screenHeight * TOP_CAPTION_Y_RATIO

    val mainTitleY =
        screenHeight * MAIN_TITLE_Y_RATIO

    val lottieTopY =
        screenHeight * LOTTIE_TOP_Y_RATIO

    val resultTitleY =
        screenHeight * RESULT_TITLE_Y_RATIO

    /*
     * ============================
     * Greeting
     * ============================
     */
    val greetingY by animateDpAsState(
        targetValue = when (phase) {
            Greeting -> {
                greetingBaseY
            }

            Intro -> {
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
            Greeting,
            Intro,
                -> 1f

            else -> 0f
        },
        animationSpec = onboardingTween(),
        label = "greetingAlpha",
    )

    /*
     * ============================
     * Intro description
     * ============================
     */
    val introDescriptionY by animateDpAsState(
        targetValue = when (phase) {
            Greeting -> {
                greetingBaseY + 48.dp
            }

            Intro -> {
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
            Intro,
            Upload,
                -> 1f

            else -> 0f
        },
        animationSpec = onboardingTween(),
        label = "introDescriptionAlpha",
    )

    /*
     * ============================
     * Upload title
     * ============================
     */
    val uploadTitleY by animateDpAsState(
        targetValue = when (phase) {
            Greeting,
            Intro,
                -> {
                mainTitleY + 18.dp
            }

            Upload -> {
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
            Upload,
            Analysis,
                -> 1f

            else -> 0f
        },
        animationSpec = onboardingTween(),
        label = "uploadTitleAlpha",
    )

    val uploadTitleScale by animateFloatAsState(
        targetValue = if (
            phase == Analysis
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
            phase == Analysis
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
     * ============================
     * Analysis title
     * ============================
     */
    val analysisTitleY by animateDpAsState(
        targetValue = when {
            phase.isResultPhase -> {
                topCaptionY
            }

            phase == Analysis -> {
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
            phase == Analysis ||
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
     * ============================
     * Glow
     * ============================
     */
    val glowY by animateDpAsState(
        targetValue = when {
            phase == Greeting ||
                    phase == Intro -> {
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

    PulsingGradientCircle(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(
                y = glowY,
            )
            .fillMaxWidth(),
    )

    /*
     * 반가워요!
     */
    Text(
        text = stringResource(
            R.string.onboarding_greeting,
        ),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(
                y = greetingY,
            )
            .graphicsLayer {
                alpha = greetingAlpha
            },
        style = ModeraTheme.typography.titleB20,
        color = ModeraTheme.colors.gray900,
        textAlign = TextAlign.Center,
    )

    /*
     * 사용 방법에 대해 간단하게 살펴볼까요?
     */
    Text(
        text = stringResource(
            R.string.onboarding_intro_description,
        ),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(
                y = introDescriptionY,
            )
            .graphicsLayer {
                alpha = introDescriptionAlpha
            },
        style = ModeraTheme.typography.bodyR16,
        color = ModeraTheme.colors.gray700,
        textAlign = TextAlign.Center,
    )

    /*
     * 잊고 있었던 스크린샷들을 올려보세요!
     */
    Text(
        text = stringResource(
            R.string.onboarding_upload_title,
        ),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(
                y = uploadTitleY,
            )
            .graphicsLayer {
                alpha = uploadTitleAlpha
                scaleX = uploadTitleScale
                scaleY = uploadTitleScale
            }
            .padding(
                horizontal = 24.dp,
            ),
        style = ModeraTheme.typography.titleB22,
        color = uploadTitleColor,
        textAlign = TextAlign.Center,
    )

    /*
     * AI가 사진을 분석해서
     * 정리해드립니다!
     */
    Text(
        text = stringResource(
            R.string.onboarding_analysis_title,
        ),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(
                y = analysisTitleY,
            )
            .fillMaxWidth()
            .graphicsLayer {
                alpha = analysisTitleAlpha
                scaleX = analysisTitleScale
                scaleY = analysisTitleScale
            }
            .padding(
                horizontal = 24.dp,
            ),
        style = ModeraTheme.typography.titleB22,
        color = analysisTitleColor,
        textAlign = TextAlign.Center,
    )

    /*
     * Scanning Lottie
     */
    if (
        (
                phase == Upload ||
                        phase == Analysis
                ) &&
        lottieComposition != null
    ) {
        LottieAnimation(
            composition = lottieComposition,
            progress = {
                lottieProgress
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    y = lottieTopY,
                )
                .fillMaxWidth()
                .aspectRatio(1f),
        )
    }
}

private fun <T> onboardingTween(
    durationMillis: Int = DEFAULT_ANIMATION_DURATION_MILLIS,
) = tween<T>(
    durationMillis = durationMillis,
    easing = FastOutSlowInEasing,
)

private const val GREETING_BASE_Y_RATIO = 0.43f
private const val TOP_CAPTION_Y_RATIO = 0.10f
private const val MAIN_TITLE_Y_RATIO = 0.25f
private const val LOTTIE_TOP_Y_RATIO = 0.33f

/*
 * AnalysisResultSection의 title 위치와 동일.
 */
private const val RESULT_TITLE_Y_RATIO = 0.245f

private const val DEFAULT_ANIMATION_DURATION_MILLIS = 500
private const val GLOW_MOVE_DURATION_MILLIS = 600
private const val TITLE_MOVE_DURATION_MILLIS = 520

private const val CAPTION_SCALE = 0.72f