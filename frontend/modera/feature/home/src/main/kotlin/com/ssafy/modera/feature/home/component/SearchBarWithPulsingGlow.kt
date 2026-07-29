package com.ssafy.modera.feature.home.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import com.ssafy.modera.core.component.PulsingGradientCircle
import kotlin.math.roundToInt

@Composable
internal fun SearchBarWithPulsingGlow(
    circleOffsetYPx: Float,
    modifier: Modifier = Modifier,
    searchBar: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            PulsingGradientCircle(
                modifier = Modifier.layoutId(PulsingGlowLayoutId.Circle),
            )
            searchBar()
        },
    ) { measurables, constraints ->
        val searchBarPlaceable = measurables
            .first { it.layoutId != PulsingGlowLayoutId.Circle }
            .measure(constraints)

        val circlePlaceable = measurables
            .first { it.layoutId == PulsingGlowLayoutId.Circle }
            .measure(constraints.copy(maxHeight = Constraints.Infinity))

        layout(searchBarPlaceable.width, searchBarPlaceable.height) {
            circlePlaceable.placeRelative(
                x = (searchBarPlaceable.width - circlePlaceable.width) / 2,
                y = ((searchBarPlaceable.height - circlePlaceable.height) / 2f + circleOffsetYPx).roundToInt(),
            )
            searchBarPlaceable.placeRelative(0, 0)
        }
    }
}

private object PulsingGlowLayoutId {
    const val Circle = "circle"
}
