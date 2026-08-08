package com.ssafy.modera.feature.onboarding.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.onboarding.api.navigation.OnboardingNavKey
import com.ssafy.modera.feature.onboarding.impl.OnboardingScreen

fun EntryProviderScope<NavKey>.onboardingEntry(
    navigator: Navigator,
    onSkipClick: () -> Unit,
    onAnalysisResultClick: () -> Unit,
    onRegisterPhotoClick: (
        onImagesPicked: () -> Unit,
    ) -> Unit,
) {
    entry<OnboardingNavKey> {
        OnboardingScreen(
            onSkipClick = onSkipClick,
            onAnalysisResultClick = onAnalysisResultClick,
            onRegisterPhotoClick = onRegisterPhotoClick,
        )
    }
}