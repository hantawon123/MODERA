package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import com.ssafy.modera.core.component.PulsingGradientCircle
import com.ssafy.modera.core.component.item.ModeraSearchBar
import com.ssafy.modera.core.component.item.SearchBarMode
import com.ssafy.modera.feature.home.R
import kotlin.math.roundToInt

@Composable
internal fun HomeSearchBarSection(
    modifier: Modifier = Modifier,
    query: String,
    circleExtraOffsetPx: Float,
    searchFocusRequester: FocusRequester,
    positionModifier: Modifier,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSearch: () -> Unit,
) {
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .then(positionModifier),
        content = {
            PulsingGradientCircle(
                modifier = Modifier.layoutId(PulsingGlowLayoutId.Circle),
            )

            ModeraSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(R.string.home_search_placeholder),
                mode = SearchBarMode.Ai,
                focusRequester = searchFocusRequester,
                onFocusChanged = onFocusChange,
                onSearch = onSearch,
                modifier = Modifier.fillMaxWidth(),
            )
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
                y = ((searchBarPlaceable.height - circlePlaceable.height) / 2f + circleExtraOffsetPx).roundToInt(),
            )
            searchBarPlaceable.placeRelative(0, 0)
        }
    }
}

private object PulsingGlowLayoutId {
    const val Circle = "circle"
}
