package com.ssafy.modera.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.ssafy.modera.core.designsystem.R

@Immutable
data class ModeraColors(
    val blue: Color,
    val white: Color,
    val gray: Color,
    val typo: Color,
) {
    companion object {
        /**
         * Provides the default colors for the light mode of the app.
         */
        @Composable
        fun defaultColors(): ModeraColors = ModeraColors(
            blue = colorResource(id = R.color.blue),
            white = colorResource(id = R.color.white),
            gray = colorResource(id = R.color.gray),
            typo = colorResource(id = R.color.typo),
        )
    }
}
