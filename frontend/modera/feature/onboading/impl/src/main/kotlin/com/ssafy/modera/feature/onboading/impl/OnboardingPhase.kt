package com.ssafy.modera.feature.onboading.impl

internal enum class OnboardingPhase {
    Greeting,
    Intro,
    Upload,
    Analysis,
    CategoryHighlight,
    HashtagHighlight,
    SummaryHighlight,
    RelatedHighlight,
    PhotoRegister,
    FeaturePager,
}

internal val OnboardingPhase.isResultPhase: Boolean
    get() = when (this) {
        OnboardingPhase.CategoryHighlight,
        OnboardingPhase.HashtagHighlight,
        OnboardingPhase.SummaryHighlight,
        OnboardingPhase.RelatedHighlight,
            -> true

        else -> false
    }

internal val OnboardingPhase.isLottiePhase: Boolean
    get() = when (this) {
        OnboardingPhase.Upload,
        OnboardingPhase.Analysis,
            -> true

        else -> false
    }

internal val OnboardingPhase.isGuideVisible: Boolean
    get() = when (this) {
        OnboardingPhase.PhotoRegister,
        OnboardingPhase.FeaturePager,
            -> false

        else -> true
    }