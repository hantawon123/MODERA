package com.ssafy.modera.feature.analyzedimagedetail.component

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
internal fun OcrTextSection(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = ModeraTheme.typography.titleSB18.copy(
                color = ModeraTheme.colors.gray900,
            ),
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = content,
            style = ModeraTheme.typography.bodyR16.copy(
                color = ModeraTheme.colors.gray700,
            ),
        )
    }
}

@Preview(
    name = "OcrTextSection",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun OcrTextSectionPreview() {
    ModeraTheme {
        OcrTextSection(
            title = "추출된 텍스트",
            content = """
                2026 대학생 연합 해커톤
                
                모집 기간: 2026.07.20 ~ 2026.08.02
                참가 대상: 대학생 및 취업 준비생
                장소: SSAFY 구미 캠퍼스
            """.trimIndent(),
            modifier = Modifier.padding(16.dp),
        )
    }
}