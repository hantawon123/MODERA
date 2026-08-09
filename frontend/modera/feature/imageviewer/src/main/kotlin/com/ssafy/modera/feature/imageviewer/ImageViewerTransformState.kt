package com.ssafy.modera.feature.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

private const val MIN_IMAGE_SCALE = 1f
private const val MAX_IMAGE_SCALE = 5f
private const val PAN_SPEED_MULTIPLIER = 2f

@Stable
internal class ImageViewerTransformState {

    var scale by mutableFloatStateOf(MIN_IMAGE_SCALE)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    private var containerSize by mutableStateOf(
        IntSize.Zero,
    )

    private var sourceImageSize by mutableStateOf(
        IntSize.Zero,
    )

    fun updateContainerSize(size: IntSize) {
        containerSize = size
        coerceCurrentOffset()
    }

    fun updateSourceImageSize(
        width: Int,
        height: Int,
    ) {
        if (width <= 0 || height <= 0) {
            return
        }

        sourceImageSize = IntSize(
            width = width,
            height = height,
        )

        coerceCurrentOffset()
    }

    fun transform(
        zoomChange: Float,
        panChange: Offset,
    ) {
        val updatedScale = (
                scale * zoomChange
                ).coerceIn(
                minimumValue = MIN_IMAGE_SCALE,
                maximumValue = MAX_IMAGE_SCALE,
            )

        val acceleratedPan = Offset(
            x = panChange.x * PAN_SPEED_MULTIPLIER,
            y = panChange.y * PAN_SPEED_MULTIPLIER,
        )

        val updatedOffset = if (
            updatedScale > MIN_IMAGE_SCALE
        ) {
            offset + acceleratedPan
        } else {
            Offset.Zero
        }

        val panBounds = calculatePanBounds(
            containerSize = containerSize,
            sourceImageSize = sourceImageSize,
            scale = updatedScale,
        )

        scale = updatedScale

        offset = if (
            updatedScale <= MIN_IMAGE_SCALE
        ) {
            Offset.Zero
        } else {
            updatedOffset.coerceWithin(
                bounds = panBounds,
            )
        }
    }

    private fun coerceCurrentOffset() {
        if (scale <= MIN_IMAGE_SCALE) {
            offset = Offset.Zero
            return
        }

        val panBounds = calculatePanBounds(
            containerSize = containerSize,
            sourceImageSize = sourceImageSize,
            scale = scale,
        )

        offset = offset.coerceWithin(
            bounds = panBounds,
        )
    }
}

@Composable
internal fun rememberImageViewerTransformState(
    imageUrl: String,
): ImageViewerTransformState {
    return remember(imageUrl) {
        ImageViewerTransformState()
    }
}

private fun calculatePanBounds(
    containerSize: IntSize,
    sourceImageSize: IntSize,
    scale: Float,
): Offset {
    if (
        containerSize.width <= 0 ||
        containerSize.height <= 0 ||
        sourceImageSize.width <= 0 ||
        sourceImageSize.height <= 0
    ) {
        return Offset.Zero
    }

    val containerWidth =
        containerSize.width.toFloat()

    val containerHeight =
        containerSize.height.toFloat()

    val sourceWidth =
        sourceImageSize.width.toFloat()

    val sourceHeight =
        sourceImageSize.height.toFloat()

    /*
     * ContentScale.Fit이 적용된 이미지의 기본 크기.
     */
    val fitScale = min(
        a = containerWidth / sourceWidth,
        b = containerHeight / sourceHeight,
    )

    val scaledWidth =
        sourceWidth * fitScale * scale

    val scaledHeight =
        sourceHeight * fitScale * scale

    return Offset(
        x = (
                scaledWidth - containerWidth
                )
            .div(2f)
            .coerceAtLeast(0f),
        y = (
                scaledHeight - containerHeight
                )
            .div(2f)
            .coerceAtLeast(0f),
    )
}

private fun Offset.coerceWithin(
    bounds: Offset,
): Offset {
    return Offset(
        x = x.coerceIn(
            minimumValue = -bounds.x,
            maximumValue = bounds.x,
        ),
        y = y.coerceIn(
            minimumValue = -bounds.y,
            maximumValue = bounds.y,
        ),
    )
}