package com.ssafy.modera.feature.document.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraDocumentInfoRow
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.document.Document

@Composable
internal fun DocumentItem(
    document: Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = document.title,
            style = ModeraTheme.typography.bodySB16,
            color = ModeraTheme.colors.gray900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(
            modifier = Modifier.height(4.dp),
        )

        Text(
            text = document.content,
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray700,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(
            modifier = Modifier.height(12.dp),
        )

        ModeraDocumentInfoRow(
            imageCount = document.sourceImageCount,
            updatedAt = document.updatedAt,
        )
    }
}

@Preview(name = "Document Item", showBackground = true)
@Composable
private fun DocumentItemPreview() {
    ModeraTheme {
        DocumentItem(
            document = Document(
                id = 1L,
                title = "오사카 3박 4일 여행 계획",
                content = "항공권, 숙소, 맛집 정보를 분석해 날짜별 일정과 추천 코스로 정리했어요.",
                sourceImageCount = 8,
                updatedAt = 1_785_114_000_000L,
            ),
            onClick = {},
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}