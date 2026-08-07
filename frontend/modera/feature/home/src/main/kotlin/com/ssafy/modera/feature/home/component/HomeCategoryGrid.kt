package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.model.category.Category
import com.ssafy.modera.feature.home.HomeScreenDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

private const val ROW_COUNT = 2
private const val VISIBLE_COLUMN_COUNT = 3

private val ITEM_WIDTH_REDUCTION = 4.dp

private const val INFINITE_ITEM_COUNT = Int.MAX_VALUE

private val AUTO_SCROLL_SPEED = 60.dp
private const val AUTO_SCROLL_RESUME_DELAY_MILLIS = 400L

private const val MIN_SCALE = 0.94f
private const val SCALE_DEAD_ZONE = 0.10f

private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val MAX_FRAME_DELTA_SECONDS = 0.05f

@Composable
internal fun HomeCategoryGrid(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return

    val columns = remember(categories) {
        categories.chunked(ROW_COUNT)
    }

    val columnCount = columns.size
    val shouldAutoScroll =
        columnCount > VISIBLE_COLUMN_COUNT

    val infiniteStartIndex = remember(columnCount) {
        calculateInfiniteStartIndex(
            itemCount = columnCount,
        )
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (shouldAutoScroll) {
            infiniteStartIndex
        } else {
            0
        },
    )

    val density = LocalDensity.current
    val autoScrollSpeedPxPerSecond = remember(density) {
        with(density) {
            AUTO_SCROLL_SPEED.toPx()
        }
    }

    var isUserTouching by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        columnCount,
        shouldAutoScroll,
    ) {
        listState.scrollToItem(
            index = if (shouldAutoScroll) {
                infiniteStartIndex
            } else {
                0
            },
        )
    }

    LaunchedEffect(
        shouldAutoScroll,
        isUserTouching,
    ) {
        if (!shouldAutoScroll || isUserTouching) {
            return@LaunchedEffect
        }

        delay(AUTO_SCROLL_RESUME_DELAY_MILLIS.milliseconds)

        while (
            listState.isScrollInProgress &&
            isActive
        ) {
            delay(50.milliseconds)
        }

        var previousFrameNanos =
            withFrameNanos { frameTimeNanos ->
                frameTimeNanos
            }

        while (isActive) {
            val currentFrameNanos = withFrameNanos { frameTimeNanos ->
                frameTimeNanos
            }

            val elapsedSeconds = ((currentFrameNanos - previousFrameNanos) / NANOS_PER_SECOND)
                .toFloat()
                .coerceAtMost(MAX_FRAME_DELTA_SECONDS)

            previousFrameNanos = currentFrameNanos

            listState.scrollBy(
                value = autoScrollSpeedPxPerSecond *
                        elapsedSeconds,
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 32.dp,
                bottom = 60.dp,
            ),
    ) {
        val spacing =
            HomeScreenDefaults.CategoryGridSpacing

        val itemWidth = (maxWidth - spacing * 2 * (VISIBLE_COLUMN_COUNT - 1)) /
                VISIBLE_COLUMN_COUNT - ITEM_WIDTH_REDUCTION

        val itemCount = if (shouldAutoScroll) {
            INFINITE_ITEM_COUNT
        } else {
            columnCount
        }

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(
                horizontal = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )

                        isUserTouching = true

                        try {
                            waitForUpOrCancellation(
                                pass = PointerEventPass.Initial,
                            )
                        } finally {
                            isUserTouching = false
                        }
                    }
                },
        ) {
            items(
                count = itemCount,
                key = { it },
            ) { index ->
                val actualIndex =
                    if (shouldAutoScroll) {
                        index % columnCount
                    } else {
                        index
                    }

                CategoryColumn(
                    categories = columns[actualIndex],
                    itemWidth = itemWidth,
                    spacing = spacing,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.carouselDepthEffect(
                        listState = listState,
                        itemIndex = index,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CategoryColumn(
    categories: List<Category>,
    itemWidth: Dp,
    spacing: Dp,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(itemWidth),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        categories.forEach { category ->
            CategoryItem(
                title = category.title,
                imageUrl = category.thumbnailUrl,
                isNew = category.isNew,
                onClick = { onCategoryClick(category) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun Modifier.carouselDepthEffect(
    listState: LazyListState,
    itemIndex: Int,
): Modifier =
    graphicsLayer {
        val layoutInfo =
            listState.layoutInfo

        val itemInfo =
            layoutInfo.visibleItemsInfo
                .firstOrNull { visibleItem ->
                    visibleItem.index == itemIndex
                } ?: return@graphicsLayer

        val viewportCenter =
            (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f

        val viewportHalfWidth =
            (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f

        if (viewportHalfWidth <= 0f) {
            return@graphicsLayer
        }

        val itemCenter = itemInfo.offset + itemInfo.size / 2f

        val distanceFraction =
            abs((itemCenter - viewportCenter) / viewportHalfWidth)
                .coerceIn(
                    minimumValue = 0f,
                    maximumValue = 1f,
                )

        val edgeFraction = ((distanceFraction - SCALE_DEAD_ZONE) / (1f - SCALE_DEAD_ZONE))
            .coerceIn(minimumValue = 0f, maximumValue = 1f)

        val easedFraction = edgeFraction * edgeFraction

        val scale = 1f - (1f - MIN_SCALE) * easedFraction

        scaleX = scale
        scaleY = scale
    }

private fun calculateInfiniteStartIndex(
    itemCount: Int,
): Int {
    val middle =
        INFINITE_ITEM_COUNT / 2

    return middle -
            middle % itemCount
}