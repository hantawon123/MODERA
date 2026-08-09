package com.ssafy.modera.feature.home.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import com.ssafy.modera.feature.home.HomeScreenDefaults

internal data class HomeSearchLayoutState(
    val upperWeight: Float,
    val upperContentAlpha: Float,
    val categoryContentAlpha: Float,
    val circleExtraOffsetPx: Float,
    val screenHeightModifier: Modifier,
    val searchBarPositionModifier: Modifier,
)

@Composable
internal fun rememberHomeSearchLayoutState(
    isSearchActive: Boolean,
): HomeSearchLayoutState {
    var screenHeightPx by remember { mutableFloatStateOf(0f) }
    var searchBarCenterPx by remember { mutableFloatStateOf(0f) }

    val upperWeight by animateFloatAsState(
        targetValue = if (isSearchActive) {
            HomeScreenDefaults.CollapsedUpperWeight
        } else {
            HomeScreenDefaults.ExpandedUpperWeight
        },
        animationSpec = tween(
            durationMillis = HomeScreenDefaults.LayoutAnimationDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "homeSearchUpperWeight",
    )
    val upperContentAlpha by animateFloatAsState(
        targetValue = if (isSearchActive) 0f else 1f,
        animationSpec = tween(
            durationMillis = HomeScreenDefaults.ContentFadeDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "homeSearchUpperAlpha",
    )
    val categoryContentAlpha by animateFloatAsState(
        targetValue = if (isSearchActive) 0f else 1f,
        animationSpec = tween(
            durationMillis = HomeScreenDefaults.ContentFadeDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "homeSearchCategoryAlpha",
    )
    val circleExtraOffsetPx by animateFloatAsState(
        targetValue = if (isSearchActive) {
            0f
        } else {
            (screenHeightPx / 2f) - searchBarCenterPx
        },
        animationSpec = tween(
            durationMillis = HomeScreenDefaults.LayoutAnimationDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "pulseCircleExtraOffset",
    )

    return HomeSearchLayoutState(
        upperWeight = upperWeight,
        upperContentAlpha = upperContentAlpha,
        categoryContentAlpha = categoryContentAlpha,
        circleExtraOffsetPx = circleExtraOffsetPx,
        screenHeightModifier = Modifier.onGloballyPositioned { coordinates ->
            screenHeightPx = coordinates.size.height.toFloat()
        },
        searchBarPositionModifier = Modifier.onGloballyPositioned { coordinates ->
            searchBarCenterPx =
                coordinates.boundsInParent().top + (coordinates.size.height / 2f)
        },
    )
}
