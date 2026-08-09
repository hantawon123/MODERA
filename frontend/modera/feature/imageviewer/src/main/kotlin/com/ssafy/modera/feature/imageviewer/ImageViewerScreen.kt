package com.ssafy.modera.feature.imageviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
fun ImageViewerScreen(
    imageUrl: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isTopBarVisible by remember {
        mutableStateOf(true)
    }

    val imageTransformState =
        rememberImageViewerTransformState(
            imageUrl = imageUrl,
        )

    val transformableState =
        rememberTransformableState {
                zoomChange,
                panChange,
                _,
            ->
            imageTransformState.transform(
                zoomChange = zoomChange,
                panChange = panChange,
            )
        }

    val backgroundColor = if (isTopBarVisible) {
        ModeraTheme.colors.white
    } else {
        ModeraTheme.colors.black
    }

    with(sharedTransitionScope) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .sharedBounds(
                    sharedContentState =
                        rememberSharedContentState(
                            key = "image-$imageUrl",
                        ),
                    animatedVisibilityScope =
                        animatedVisibilityScope,
                )
                .background(backgroundColor)
                .onSizeChanged(
                    imageTransformState::updateContainerSize,
                )
                .clipToBounds(),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(
                    R.string
                        .image_viewer_original_image,
                ),
                contentScale = ContentScale.Fit,
                onSuccess = { success ->
                    val drawable =
                        success.result.drawable

                    imageTransformState
                        .updateSourceImageSize(
                            width =
                                drawable.intrinsicWidth,
                            height =
                                drawable.intrinsicHeight,
                        )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX =
                            imageTransformState.scale
                        scaleY =
                            imageTransformState.scale
                        translationX =
                            imageTransformState.offset.x
                        translationY =
                            imageTransformState.offset.y
                    }
                    .transformable(
                        state = transformableState,
                        lockRotationOnZoomPan = true,
                    )
                    .pointerInput(imageUrl) {
                        detectTapGestures(
                            onTap = {
                                isTopBarVisible =
                                    !isTopBarVisible
                            },
                        )
                    },
            )

            if (isTopBarVisible) {
                ModeraTopBar(
                    onBackClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .background(ModeraTheme.colors.white),
                )
            }
        }
    }
}

@Preview(
    name = "Image Viewer Screen",
    showBackground = true,
)
@Composable
private fun ImageViewerScreenPreview() {
    ModeraTheme {
        SharedTransitionLayout {
            AnimatedVisibility(
                visible = true,
            ) {
                ImageViewerScreen(
                    imageUrl =
                        "https://picsum.photos/seed/image-viewer/600/1000",
                    sharedTransitionScope =
                        this@SharedTransitionLayout,
                    animatedVisibilityScope =
                        this@AnimatedVisibility,
                    onBackClick = {},
                )
            }
        }
    }
}