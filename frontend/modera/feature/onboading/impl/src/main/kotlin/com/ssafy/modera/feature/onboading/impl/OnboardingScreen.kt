package com.ssafy.modera.feature.onboading.impl

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.onboading.impl.component.AnalysisResultSection
import com.ssafy.modera.feature.onboading.impl.component.OnboardingGuideSection
import com.ssafy.modera.feature.onboading.impl.component.OnboardingSkipButton
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OnboardingScreen(
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var phase by remember {
        mutableStateOf(OnboardingPhase.Greeting)
    }

    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            R.raw.lottie_scanning_screen,
        ),
    )

    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        isPlaying = phase == OnboardingPhase.Upload ||
                phase == OnboardingPhase.Analysis,
        iterations = 1,
    )

    /*
     * Greeting
     * ↓
     * Intro
     * ↓
     * Upload
     */
    LaunchedEffect(Unit) {
        delay(GREETING_DURATION_MILLIS.milliseconds)

        phase = OnboardingPhase.Intro

        delay(INTRO_DURATION_MILLIS.milliseconds)

        phase = OnboardingPhase.Upload
    }

    /*
     * Upload
     * ↓
     * Analysis
     * ↓
     * CategoryHighlight
     */
    LaunchedEffect(
        phase,
        lottieProgress,
    ) {
        when {
            phase == OnboardingPhase.Upload &&
                    lottieProgress >= ANALYSIS_PHASE_PROGRESS -> {
                phase = OnboardingPhase.Analysis
            }

            phase == OnboardingPhase.Analysis &&
                    lottieProgress >= RESULT_PHASE_PROGRESS -> {
                phase = OnboardingPhase.CategoryHighlight
            }
        }
    }

    /*
     * Category
     * ↓
     * Hashtag
     * ↓
     * Summary
     * ↓
     * Related
     */
    LaunchedEffect(phase) {
        when (phase) {
            OnboardingPhase.CategoryHighlight -> {
                delay(HIGHLIGHT_DURATION_MILLIS.milliseconds)

                phase = OnboardingPhase.HashtagHighlight
            }

            OnboardingPhase.HashtagHighlight -> {
                delay(HIGHLIGHT_DURATION_MILLIS.milliseconds)

                phase = OnboardingPhase.SummaryHighlight
            }

            OnboardingPhase.SummaryHighlight -> {
                delay(HIGHLIGHT_DURATION_MILLIS.milliseconds)

                phase = OnboardingPhase.RelatedHighlight
            }

            else -> Unit
        }
    }

    OnboardingContent(
        phase = phase,
        lottieProgress = lottieProgress,
        lottieComposition = lottieComposition,
        onSkipClick = onSkipClick,
        modifier = modifier,
    )
}

@Composable
private fun OnboardingContent(
    phase: OnboardingPhase,
    lottieProgress: Float,
    lottieComposition: com.airbnb.lottie.LottieComposition?,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        OnboardingGuideSection(
            phase = phase,
            lottieProgress = lottieProgress,
            lottieComposition = lottieComposition,
        )

        AnalysisResultSection(
            phase = phase,
        )

        OnboardingSkipButton(
            onClick = onSkipClick,
        )
    }
}

private const val GREETING_DURATION_MILLIS = 900L
private const val INTRO_DURATION_MILLIS = 1_300L

private const val ANALYSIS_PHASE_PROGRESS = 0.33f
private const val RESULT_PHASE_PROGRESS = 0.98f

private const val HIGHLIGHT_DURATION_MILLIS = 1_600L

@Preview(
    name = "Onboarding",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
)
@Composable
private fun OnboardingScreenPreview() {
    ModeraTheme {
        OnboardingScreen(
            onSkipClick = {},
        )
    }
}