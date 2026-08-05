package com.ssafy.modera.core.ui


import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
fun EmptyScreen(
    message: String,
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int,
) {
    ErrorScreen(
        message = message,
        imageRes = imageRes,
    )
}

@Preview(
    name = "Empty Screen",
    showBackground = true,
)
@Composable
private fun EmptyScreenPreview() {
    ModeraTheme {
        EmptyScreen(
            message = "이미지 정보를 불러오지 못했습니다.",
            imageRes = R.drawable.img_character_crying,
        )
    }
}