package com.ssafy.modera.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ssafy.modera.core.designsystem.R

@OptIn(ExperimentalTextApi::class)
private fun pretendardFont(
    weight: FontWeight,
): Font = Font(
    resId = R.font.pretendard_variable,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
    ),
)

internal val pretendardFontFamily = FontFamily(
    pretendardFont(FontWeight.Light),
    pretendardFont(FontWeight.Normal),
    pretendardFont(FontWeight.Medium),
    pretendardFont(FontWeight.SemiBold),
    pretendardFont(FontWeight.Bold),
)

// TODO: 디자인 나오면 디자인에 맞게 변경해야함
@Immutable
data class ModeraTypography(
    val headline1G: TextStyle,
    val headline2G: TextStyle,
    val headline3G: TextStyle,
    val subtitle1G: TextStyle,
    val subtitle2G: TextStyle,
    val subtitle3G: TextStyle,
    val subtitle4G: TextStyle,

    val headline1: TextStyle,
    val headline2: TextStyle,
    val subtitle1: TextStyle,
    val subtitle2: TextStyle,
    val subtitle3: TextStyle,
    val subtitle3Medium: TextStyle,
    val subtitle3SemiBold: TextStyle,
    val subtitle3Bold: TextStyle,

    val body1: TextStyle,
    val body1SemiBold: TextStyle,
    val body2: TextStyle,
    val body2Medium: TextStyle,
    val body2SemiBold: TextStyle,

    val caption1: TextStyle,
    val caption1Medium: TextStyle,
    val caption1SemiBold: TextStyle,
    val caption1Bold: TextStyle
) {
    companion object {
        /**
         * Provides the default typography styles for the app.
         */
        @Composable
        fun defaultTypography(): ModeraTypography = ModeraTypography(
            headline1G = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 60.sp,
                lineHeight = 80.sp,
                letterSpacing = 0.sp
            ),
            headline2G = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp
            ),
            headline3G = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp
            ),
            subtitle1G = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 34.sp,
                letterSpacing = 0.sp
            ),
            subtitle2G = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 30.sp,
                letterSpacing = 0.sp
            ),
            subtitle3G = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp
            ),
            subtitle4G = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),

            headline1 = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 30.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp
            ),
            headline2 = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp
            ),
            subtitle1 = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                letterSpacing = 0.sp
            ),
            subtitle2 = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp
            ),
            subtitle3 = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),
            subtitle3Medium = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),
            subtitle3SemiBold = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),
            subtitle3Bold = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),

            body1 = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp
            ),
            body1SemiBold = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp
            ),
            body2 = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp
            ),
            body2Medium = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp
            ),
            body2SemiBold = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp
            ),

            caption1 = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp
            ),
            caption1Medium = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp
            ),
            caption1SemiBold = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp
            ),
            caption1Bold = TextStyle(
                fontFamily = pretendardFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp
            )
        )
    }
}
