package com.ssafy.modera.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

private val LocalModeraColors = compositionLocalOf<ModeraColors> {
    error("No colors provided!")
}

private val LocalModeraTypography = compositionLocalOf<ModeraTypography> {
    error("No ModeraTypography provided!")
}

val LocalModeraContentColor = compositionLocalOf { Color(0xFF404040) }

@Composable
fun ModeraTheme(
    colors: ModeraColors = ModeraColors.defaultColors(),
    typography: ModeraTypography = ModeraTypography.defaultTypography(),
    background: ModeraBackground = ModeraBackground.defaultBackground(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalModeraColors provides colors,
        LocalModeraTypography provides typography,
        LocalBackgroundTheme provides background,
    ) {
        Box(
            modifier = Modifier
                .background(background.color)
                .semantics { testTagsAsResourceId = true },
        ) {
            content()
        }
    }
}

object ModeraTheme {
    val colors: ModeraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalModeraColors.current

    val typography: ModeraTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalModeraTypography.current
}
