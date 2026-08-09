package com.ssafy.modera.feature.home.component

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager

@Composable
internal fun HomeSearchFocusEffect(
    isSearchActive: Boolean,
    isShowingSearchResults: Boolean,
    searchFocusRequester: FocusRequester,
    onSearchDeactivate: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var wasSearchActive by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchActive) {
        onSearchDeactivate()
        focusManager.clearFocus()
    }

    LaunchedEffect(isSearchActive) {
        when {
            isSearchActive && !wasSearchActive && !isShowingSearchResults -> {
                searchFocusRequester.requestFocus()
            }

            !isSearchActive && wasSearchActive -> {
                focusManager.clearFocus()
            }
        }
        wasSearchActive = isSearchActive
    }
}
