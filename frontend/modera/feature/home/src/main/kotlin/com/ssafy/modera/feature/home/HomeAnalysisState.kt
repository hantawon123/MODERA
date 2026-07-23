package com.ssafy.modera.feature.home

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class HomeAnalysisState(
    private val onDismissRequest: () -> Unit = {},
) {
    var showBanner by mutableStateOf(false)
        private set
    var imageCount by mutableIntStateOf(0)
        private set

    fun sync(showBanner: Boolean, imageCount: Int) {
        this.imageCount = imageCount
        this.showBanner = showBanner
    }

    fun dismissBanner() {
        showBanner = false
        onDismissRequest()
    }
}

val LocalHomeAnalysisState = compositionLocalOf<HomeAnalysisState> {
    error("HomeAnalysisState not provided")
}
