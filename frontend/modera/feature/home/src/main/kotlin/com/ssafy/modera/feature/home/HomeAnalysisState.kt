package com.ssafy.modera.feature.home

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class HomeAnalysisState {
    var showBanner by mutableStateOf(false)
    var imageCount by mutableIntStateOf(0)

    fun onImagesSelected(count: Int) {
        imageCount = count
        showBanner = count > 0
    }

    fun dismissBanner() {
        showBanner = false
    }
}

val LocalHomeAnalysisState = compositionLocalOf<HomeAnalysisState> {
    error("HomeAnalysisState not provided")
}
