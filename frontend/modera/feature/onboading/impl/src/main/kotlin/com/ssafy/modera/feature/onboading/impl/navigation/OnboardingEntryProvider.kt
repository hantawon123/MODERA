package com.ssafy.modera.feature.onboading.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.onboading.api.navigation.OnboardingNavKey
import com.ssafy.modera.feature.onboading.impl.OnboardingScreen

fun EntryProviderScope<NavKey>.onboardingEntry(
    navigator: Navigator,
    onSkipClick: () -> Unit,
    onRegisterPhotoClick: () -> Unit,
) {
    entry<OnboardingNavKey> {
        OnboardingScreen(
            onSkipClick = onSkipClick,
            onRegisterPhotoClick = onRegisterPhotoClick,
        )
    }
}