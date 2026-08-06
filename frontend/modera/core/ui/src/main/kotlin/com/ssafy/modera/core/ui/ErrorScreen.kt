package com.ssafy.modera.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
fun ErrorScreen(
    message: String,
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int = R.drawable.img_character_dizzy,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
        )

        Text(
            text = message,
            style = ModeraTheme.typography.bodyR16.copy(
                color = ModeraTheme.colors.gray700,
            ),
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

@Preview(
    name = "Error Screen",
    showBackground = true,
)
@Composable
private fun ErrorScreenPreview() {
    ModeraTheme {
        ErrorScreen(
            message = "이미지 정보를 불러오지 못했습니다.",
        )
    }
}