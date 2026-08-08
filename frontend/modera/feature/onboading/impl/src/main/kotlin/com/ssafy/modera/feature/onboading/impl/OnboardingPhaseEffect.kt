package com.ssafy.modera.feature.onboading.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val GREETING_DURATION_MILLIS = 900L
private const val INTRO_DURATION_MILLIS = 1_300L

private const val ANALYSIS_PHASE_PROGRESS = 0.33f
private const val RESULT_PHASE_PROGRESS = 0.98f

private const val HIGHLIGHT_DURATION_MILLIS = 1_000L
private const val RELATED_DURATION_MILLIS = 1_800L

@Composable
internal fun OnboardingPhaseEffect(
    phase: OnboardingPhase,
    lottieProgress: Float,
    onPhaseChange: (OnboardingPhase) -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(GREETING_DURATION_MILLIS.milliseconds)

        onPhaseChange(
            OnboardingPhase.Intro,
        )

        delay(INTRO_DURATION_MILLIS.milliseconds)

        onPhaseChange(
            OnboardingPhase.Upload,
        )
    }
    LaunchedEffect(
        phase,
        lottieProgress,
    ) {
        when {
            phase == OnboardingPhase.Upload &&
                    lottieProgress >= ANALYSIS_PHASE_PROGRESS -> {
                onPhaseChange(
                    OnboardingPhase.Analysis,
                )
            }

            phase == OnboardingPhase.Analysis &&
                    lottieProgress >= RESULT_PHASE_PROGRESS -> {
                onPhaseChange(
                    OnboardingPhase.CategoryHighlight,
                )
            }
        }
    }
    LaunchedEffect(phase) {
        when (phase) {
            OnboardingPhase.CategoryHighlight -> {
                delay(HIGHLIGHT_DURATION_MILLIS.milliseconds)

                onPhaseChange(
                    OnboardingPhase.HashtagHighlight,
                )
            }

            OnboardingPhase.HashtagHighlight -> {
                delay(HIGHLIGHT_DURATION_MILLIS.milliseconds)

                onPhaseChange(
                    OnboardingPhase.SummaryHighlight,
                )
            }

            OnboardingPhase.SummaryHighlight -> {
                delay(HIGHLIGHT_DURATION_MILLIS.milliseconds)

                onPhaseChange(
                    OnboardingPhase.RelatedHighlight,
                )
            }

            OnboardingPhase.RelatedHighlight -> {
                delay(RELATED_DURATION_MILLIS.milliseconds)

                onPhaseChange(
                    OnboardingPhase.PhotoRegister,
                )
            }

            else -> Unit
        }
    }
}