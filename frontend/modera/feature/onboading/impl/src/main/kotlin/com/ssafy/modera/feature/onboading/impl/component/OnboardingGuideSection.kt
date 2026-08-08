package com.ssafy.modera.feature.onboading.impl.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import com.ssafy.modera.feature.onboading.impl.R
import com.ssafy.modera.feature.onboading.impl.isGuideVisible
import com.ssafy.modera.feature.onboading.impl.isLottiePhase
import com.ssafy.modera.feature.onboading.impl.rememberOnboardingAnimationState

@Composable
internal fun BoxWithConstraintsScope.OnboardingGuideSection(
    phase: OnboardingPhase,
    lottieProgress: Float,
    lottieComposition: LottieComposition?,
) {
    val animationState = rememberOnboardingAnimationState(
        phase = phase,
        screenHeight = maxHeight,
    )

    AnimatedVisibility(
        visible = phase.isGuideVisible,
        modifier = Modifier.matchParentSize(),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = GUIDE_EXIT_DURATION_MILLIS,
            ),
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            /*
             * Gradient
             */
            PulsingGradientCircle(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = animationState.glowY,
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
                        y = animationState.greetingY,
                    )
                    .graphicsLayer {
                        alpha = animationState.greetingAlpha
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
                        y = animationState.introDescriptionY,
                    )
                    .graphicsLayer {
                        alpha = animationState.introDescriptionAlpha
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
                        y = animationState.uploadTitleY,
                    )
                    .graphicsLayer {
                        alpha = animationState.uploadTitleAlpha
                        scaleX = animationState.uploadTitleScale
                        scaleY = animationState.uploadTitleScale
                    }
                    .padding(
                        horizontal = 24.dp,
                    ),
                style = ModeraTheme.typography.titleB22,
                color = animationState.uploadTitleColor,
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
                        y = animationState.analysisTitleY,
                    )
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = animationState.analysisTitleAlpha
                        scaleX = animationState.analysisTitleScale
                        scaleY = animationState.analysisTitleScale
                    }
                    .padding(
                        horizontal = 24.dp,
                    ),
                style = ModeraTheme.typography.titleB22,
                color = animationState.analysisTitleColor,
                textAlign = TextAlign.Center,
            )

            /*
             * Scanning Lottie
             */
            if (
                phase.isLottiePhase &&
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
                            y = animationState.lottieTopY,
                        )
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            }
        }
    }
}

private const val GUIDE_EXIT_DURATION_MILLIS = 300