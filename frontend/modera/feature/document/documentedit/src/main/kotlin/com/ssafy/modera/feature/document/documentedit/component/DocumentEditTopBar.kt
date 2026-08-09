package com.ssafy.modera.feature.document.documentedit.component

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.documentedit.R

@Composable
internal fun DocumentEditTopBar(
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModeraTopBar(
        onBackClick = onBackClick,
        modifier = modifier,
        centerContent = {
            Text(
                text = stringResource(
                    R.string.document_edit_title,
                ),
                style = ModeraTheme.typography.bodySB16,
                color = ModeraTheme.colors.gray900,
                maxLines = 1,
            )
        },
        rightContent = {
            Text(
                text = stringResource(R.string.document_edit_edit),
                modifier = Modifier.clickable(onClick = onEditClick),
                style = ModeraTheme.typography.bodySB14,
                color = ModeraTheme.colors.blue,
            )
        },
    )
}

@Preview(name = "DocumentEditTopBar", showBackground = true)
@Composable
private fun DocumentEditTopBarPreview() {
    ModeraTheme {
        DocumentEditTopBar(
            onBackClick = {},
            onEditClick = {},
        )
    }
}