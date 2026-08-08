package com.ssafy.modera.feature.onboarding.impl.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

internal data class OnboardingAnimationState(
    val greetingY: Dp,
    val greetingAlpha: Float,

    val introDescriptionY: Dp,
    val introDescriptionAlpha: Float,

    val uploadTitleY: Dp,
    val uploadTitleAlpha: Float,
    val uploadTitleScale: Float,
    val uploadTitleColor: Color,

    val analysisTitleY: Dp,
    val analysisTitleAlpha: Float,
    val analysisTitleScale: Float,
    val analysisTitleColor: Color,

    val glowY: Dp,
    val lottieTopY: Dp,
)