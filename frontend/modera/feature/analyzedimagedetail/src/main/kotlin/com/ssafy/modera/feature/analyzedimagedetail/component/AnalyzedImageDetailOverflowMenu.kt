package com.ssafy.modera.feature.analyzedimagedetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.analyzedimagedetail.R

@Composable
internal fun AnalyzedImageDetailOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onCopyTextClick: () -> Unit,
    onViewInfoClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!expanded) return

    Popup(
        onDismissRequest = onDismissRequest,
        alignment = androidx.compose.ui.Alignment.TopEnd,
    ) {
        Column(
            modifier = modifier
                .padding(
                    top = 56.dp,
                    end = 16.dp,
                )
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(ModeraTheme.colors.white)
                .padding(vertical = 4.dp),
        ) {
            AnalyzedImageDetailMenuItem(
                text = stringResource(R.string.image_detail_copy_text),
                onClick = {
                    onCopyTextClick()
                    onDismissRequest()
                },
            )
            AnalyzedImageDetailMenuItem(
                text = stringResource(R.string.image_detail_view_info),
                onClick = {
                    onViewInfoClick()
                    onDismissRequest()
                },
            )
            AnalyzedImageDetailMenuItem(
                text = stringResource(R.string.image_detail_delete),
                textColor = Color(0xFFE53935),
                onClick = {
                    onDeleteClick()
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
private fun AnalyzedImageDetailMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = ModeraTheme.colors.gray700,
) {
    ClickableSurface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 14.dp,
            ),
            style = ModeraTheme.typography.bodyR14,
            color = textColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyzedImageDetailOverflowMenuPreview() {
    ModeraTheme {
        AnalyzedImageDetailOverflowMenu(
            expanded = true,
            onDismissRequest = {},
            onCopyTextClick = {},
            onViewInfoClick = {},
            onDeleteClick = {},
        )
    }
}
