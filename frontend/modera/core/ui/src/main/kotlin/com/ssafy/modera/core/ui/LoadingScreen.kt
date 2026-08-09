package com.ssafy.modera.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ssafy.modera.core.designsystem.component.LoadingWheel
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ModeraTheme.colors.white,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        LoadingWheel()
    }
}