package com.ssafy.modera.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.splash_logo),
    )
    val animationState = animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 2f,
    )
    val density = LocalDensity.current
    var characterHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(animationState.isAtEnd, animationState.isPlaying, composition) {
        if (composition != null && animationState.isAtEnd && !animationState.isPlaying) {
            onFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            LottieAnimation(
                composition = composition,
                progress = { animationState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
            )
            Spacer(modifier = Modifier.weight(3f))
        }

        Image(
            painter = painterResource(R.drawable.img_character_waiting),
            contentDescription = null,
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomCenter)
                .onSizeChanged { size -> characterHeightPx = size.height }
                .offset(
                    y = with(density) {
                        (characterHeightPx * 0.3f).toDp()
                    },
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    ModeraTheme {
        SplashScreen(
            onFinished = {},
        )
    }
}
