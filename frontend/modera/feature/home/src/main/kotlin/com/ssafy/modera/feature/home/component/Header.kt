package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
internal fun Header(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 60.dp)
    ) {
        Text(
            text = title,
            style = ModeraTheme.typography.titleB22,
            color = ModeraTheme.colors.gray700,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray700,
        )
    }
}

@Preview(
    name = "Home Header",
    showBackground = true,
)
@Composable
private fun HeaderPreview() {
    ModeraTheme {
        Header(
            title = "AI가 자동으로 분류해드려요",
            subtitle = "스크린샷과 사진을 내용에 맞게 정리했어요",
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}