package com.ssafy.modera.feature.analyzedimage.detail.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
internal fun AnalysisSummarySection(
    content: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = content,
            style = ModeraTheme.typography.bodyR16.copy(
                color = ModeraTheme.colors.gray700,
            ),
        )
    }
}

@Preview(
    name = "AnalysisSummarySection",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun AnalysisSummarySectionPreview() {
    ModeraTheme {
        AnalysisSummarySection(
            content = "SSAFY에서 진행되는 대학생 연합 해커톤 모집 공고입니다. "
                    + "접수 기간과 참가 대상, 주요 일정 정보를 확인할 수 있습니다.",
            modifier = Modifier.padding(16.dp),
        )
    }
}