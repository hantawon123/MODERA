package com.ssafy.modera.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.ui.R.drawable.img_character_document_empty

@Composable
fun RecommendScreen(
    title: String,
    subtitle: String,
    image: Int,
    modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier = Modifier.width(120.dp),
            )

            Spacer(
                modifier = Modifier.height(24.dp),
            )

            Text(
                text = title,
                style = ModeraTheme.typography.titleB18,
                color = ModeraTheme.colors.gray700,
                maxLines = 1,
            )

            Spacer(
                modifier = Modifier.height(14.dp),
            )

            Text(
                text = subtitle,
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray400,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "RecommendScreen ", showBackground = true)
@Composable
private fun RecommendScreenPreview() {
    ModeraTheme {
        RecommendScreen(
            title = "스크린샷을 문서로 정리해보세요!",
            subtitle = " 기존 이미지를 선택하면\nAI가 관련 이미지를 추천해드려요.",
            image = img_character_document_empty
        )
    }
}