package com.ssafy.modera.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageLoader = rememberGifImageLoader(context)

    LaunchedEffect(onFinished) {
        delay(SplashDefaults.duration)
        onFinished()
    }

    Column(

        modifier = modifier
        .fillMaxSize()
        .background(ModeraTheme.colors.gray100),
    ) {
        Spacer(Modifier.weight(3f))

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.drawable.splash)
                .build(),
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier.aspectRatio(0.5f),
            contentScale = ContentScale.Fit,
        )

        Spacer(Modifier.weight(2f))
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
