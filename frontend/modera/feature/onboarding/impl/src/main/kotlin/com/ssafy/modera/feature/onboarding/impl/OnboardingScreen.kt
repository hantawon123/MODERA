package com.ssafy.modera.feature.onboarding.impl

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.onboarding.impl.component.AnalysisResultSection
import com.ssafy.modera.feature.onboarding.impl.component.OnboardingFeaturePagerSection
import com.ssafy.modera.feature.onboarding.impl.component.OnboardingGuideSection
import com.ssafy.modera.feature.onboarding.impl.component.OnboardingSkipButton
import com.ssafy.modera.feature.onboarding.impl.component.PhotoRegisterSection
import com.ssafy.modera.feature.onboarding.impl.model.OnboardingAnalysisState

@Composable
fun OnboardingScreen(
    onSkipClick: () -> Unit,
    onRegisterPhotoClick: (
        onImagesPicked: () -> Unit,
    ) -> Unit,
    onAnalysisResultClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var phase by remember { mutableStateOf(OnboardingPhase.Greeting) }
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()

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

    OnboardingPhaseEffect(
        phase = phase,
        lottieProgress = lottieProgress,
        onPhaseChange = { newPhase -> phase = newPhase },
    )

    OnboardingContent(
        phase = phase,
        analysisState = analysisState,
        lottieProgress = lottieProgress,
        lottieComposition = lottieComposition,
        onSkipClick = onSkipClick,
        onRegisterPhotoClick = {
            onRegisterPhotoClick {
                viewModel.onAnalysisStarted()
                phase = OnboardingPhase.FeaturePager
            }
        },
        onAnalysisResultClick = onAnalysisResultClick,
        modifier = modifier,
    )
}

@Composable
private fun OnboardingContent(
    phase: OnboardingPhase,
    analysisState: OnboardingAnalysisState,
    lottieProgress: Float,
    lottieComposition: com.airbnb.lottie.LottieComposition?,
    onSkipClick: () -> Unit,
    onAnalysisResultClick: () -> Unit,
    onRegisterPhotoClick: () -> Unit,
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

        PhotoRegisterSection(
            phase = phase,
            onRegisterPhotoClick = onRegisterPhotoClick,
        )

        OnboardingFeaturePagerSection(
            phase = phase,
            onAnalysisResultClick = onAnalysisResultClick,
            analysisState = analysisState,
        )

        OnboardingSkipButton(
            onClick = onSkipClick,
        )
    }
}

@Preview(name = "Onboarding", showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    ModeraTheme {
        OnboardingScreen(
            onSkipClick = {},
            onAnalysisResultClick = {},
            onRegisterPhotoClick = {},
        )
    }
}