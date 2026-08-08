package com.ssafy.modera.feature.onboarding.impl.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraIconTextButton
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.onboarding.impl.OnboardingPhase
import com.ssafy.modera.feature.onboarding.impl.R

@Composable
internal fun BoxWithConstraintsScope.PhotoRegisterSection(
    phase: OnboardingPhase,
    onRegisterPhotoClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = phase == OnboardingPhase.PhotoRegister,
        modifier = Modifier.matchParentSize(),
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = SECTION_ENTER_DURATION_MILLIS,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = SECTION_ENTER_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetY = { height ->
                height / SECTION_ENTER_OFFSET_DIVISOR
            },
        ),
    ) {
        val screenHeight = maxHeight

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = stringResource(
                    R.string.onboarding_photo_register_title,
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = screenHeight * TITLE_Y_RATIO,
                    ),
                style = ModeraTheme.typography.titleB22,
                color = ModeraTheme.colors.gray900,
                textAlign = TextAlign.Center,
            )

            /*
             * 사진 등록하기
             */
            ModeraIconTextButton(
                text = stringResource(
                    R.string.onboarding_photo_register_button,
                ),
                icon = painterResource(
                    ModeraIcons.Add,
                ),
                onClick = onRegisterPhotoClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = screenHeight * BUTTON_Y_RATIO,
                    )
                    .padding(
                        horizontal = 24.dp,
                    )
                    .height(BUTTON_HEIGHT),
                buttonColor = ModeraTheme.colors.yellow500,
                contentColor = ModeraTheme.colors.white,
                borderColor = ModeraTheme.colors.yellow500,
                iconContentDescription = null,
            )

            /*
             * 캐릭터
             */
            Image(
                painter = painterResource(
                    R.drawable.img_onboarding_register_character,
                ),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = screenHeight * CHARACTER_Y_RATIO,
                    )
                    .size(CHARACTER_SIZE),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private const val TITLE_Y_RATIO = 0.245f
private const val BUTTON_Y_RATIO = 0.595f
private const val CHARACTER_Y_RATIO = 0.755f

private val BUTTON_HEIGHT = 52.dp
private val CHARACTER_SIZE = 140.dp

private const val SECTION_ENTER_DURATION_MILLIS = 500
private const val SECTION_ENTER_OFFSET_DIVISOR = 16