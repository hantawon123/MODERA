package com.ssafy.modera.core.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.common.datetime.ModeraDateFormatter
import com.ssafy.modera.core.common.datetime.ModeraDateStyle
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.ui.R

@Composable
fun ModeraDocumentInfoRow(
    imageCount: Int,
    updatedAt: Long,
    modifier: Modifier = Modifier,
    isCountingVisible: Boolean = true,
    contentColor: Color = ModeraTheme.colors.gray400,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCountingVisible) {
            InfoItem(
                icon = ModeraIcons.Image,
                text = stringResource(
                    R.string.document_source_image_count,
                    imageCount,
                ),
                contentColor = contentColor,
            )
        }

        InfoItem(
            icon = ModeraIcons.Clock,
            text = stringResource(
                R.string.document_updated_at,
                ModeraDateFormatter.formatMillis(
                    timestampMillis = updatedAt,
                    style = ModeraDateStyle.SMART,
                ),
            ),
            contentColor = contentColor,
        )
    }
}

@Composable
private fun InfoItem(
    icon: Int,
    text: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = contentColor,
        )

        Text(
            text = text,
            style = ModeraTheme.typography.captionR12,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Preview(name = "Modera Document Info Row", showBackground = true)
@Composable
private fun ModeraDocumentInfoRowPreview() {
    ModeraTheme {
        ModeraDocumentInfoRow(
            imageCount = 8,
            updatedAt = 1_785_593_950_561L,
            modifier = Modifier
                .background(ModeraTheme.colors.white)
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
        )
    }
}