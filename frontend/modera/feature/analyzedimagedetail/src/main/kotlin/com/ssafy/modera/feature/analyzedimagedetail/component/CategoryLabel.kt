package com.ssafy.modera.feature.analyzedimagedetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
internal fun CategoryLabel(
    category: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ModeraTheme.colors.yellow700Bg)
            .padding(
                horizontal = 12.dp,
                vertical = 7.dp,
            ),
    ) {
        Text(
            text = category,
            style = ModeraTheme.typography.captionSB12.copy(
                color = ModeraTheme.colors.yellow800,
            ),
        )
    }
}

@Preview(
    name = "CategoryLabel",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun CategoryLabelPreview() {
    ModeraTheme {
        CategoryLabel(
            category = "금융",
            modifier = Modifier.padding(16.dp),
        )
    }
}