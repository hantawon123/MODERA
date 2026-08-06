package com.ssafy.modera.feature.analyzedimage.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
internal fun KeyInformationSection(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = ModeraTheme.typography.bodySB16,
            color = ModeraTheme.colors.gray900,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { item ->
                KeyInformationItem(
                    text = item,
                )
            }
        }
    }
}

@Composable
private fun KeyInformationItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = ModeraTheme.typography.bodyR16,
            color = ModeraTheme.colors.yellow800,
        )

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = ModeraTheme.typography.bodyR16,
            color = ModeraTheme.colors.gray700,
        )
    }
}

@Preview(
    name = "KeyInformationSection",
    showBackground = true,
)
@Composable
private fun KeyInformationSectionPreview() {
    ModeraTheme {
        KeyInformationSection(
            title = "핵심 정보",
            items = listOf(
                "교육 기관: SSAFY",
                "교육 목표: 진짜 프로그램을 잘 짜는 인력 양성",
                "핵심 역량 1: 본인이 직접 프로그램을 작성하는 능력 (코딩 테스트로 검증)",
                "핵심 역량 2: 정확성과 성능을 생각할 수 있는 역량",
            ),
            modifier = Modifier.padding(24.dp),
        )
    }
}