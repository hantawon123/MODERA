package com.ssafy.modera.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.ssafy.modera.core.designsystem.R

@Immutable
data class ModeraColors(
    val white: Color,
    val gray50: Color,
    val gray100: Color,
    val gray200: Color,
    val gray300: Color,
    val gray400: Color,
    val gray500: Color,
    val gray700: Color,
    val gray900: Color,
    val black: Color,
    val yellow500: Color,
    val yellow600: Color,
    val yellow700: Color,
    val yellow800: Color,
    val yellow700Bg: Color,
    val yellow900Bg: Color,
    val kakaoYellow: Color,
    val red: Color,
    val blue: Color,
    val brown: Color,
) {
    companion object {
        /**
         * Provides the default colors for the light mode of the app.
         */
        @Composable
        fun defaultColors(): ModeraColors = ModeraColors(
            white = colorResource(R.color.white),
            gray50 = colorResource(R.color.gray_50),
            gray100 = colorResource(R.color.gray_100),
            gray200 = colorResource(R.color.gray_200),
            gray300 = colorResource(R.color.gray_300),
            gray400 = colorResource(R.color.gray_400),
            gray500 = colorResource(R.color.gray_500),
            gray700 = colorResource(R.color.gray_700),
            gray900 = colorResource(R.color.gray_900),
            black = colorResource(R.color.black),
            yellow500 = colorResource(R.color.yellow_500),
            yellow600 = colorResource(R.color.yellow_600),
            yellow700 = colorResource(R.color.yellow_700),
            yellow800 = colorResource(R.color.yellow_800),
            yellow700Bg = colorResource(R.color.yellow_700_bg),
            yellow900Bg = colorResource(R.color.yellow_900_bg),
            kakaoYellow = colorResource(R.color.kakao_yellow),
            red = colorResource(R.color.red),
            blue = colorResource(R.color.blue),
            brown = colorResource(R.color.brown),
        )
    }
}
