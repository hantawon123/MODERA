package com.ssafy.modera.feature.documentedit.component

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
    isEditing: Boolean,
    actionVisible: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onApplyClick: () -> Unit,
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
            if (actionVisible) {
                Text(
                    text = stringResource(
                        if (isEditing) {
                            R.string.document_edit_apply
                        } else {
                            R.string.document_edit_edit
                        },
                    ),
                    modifier = Modifier.clickable(
                        onClick = if (isEditing) {
                            onApplyClick
                        } else {
                            onEditClick
                        },
                    ),
                    style = ModeraTheme.typography.bodySB14,
                    color = ModeraTheme.colors.blue,
                )
            }
        },
    )
}

@Preview(name = "일반 상태", showBackground = true)
@Composable
private fun DocumentEditTopBarPreview() {
    ModeraTheme {
        DocumentEditTopBar(
            isEditing = false,
            actionVisible = true,
            onBackClick = {},
            onEditClick = {},
            onApplyClick = {},
        )
    }
}

@Preview(name = "편집 상태", showBackground = true)
@Composable
private fun DocumentEditTopBarEditingPreview() {
    ModeraTheme {
        DocumentEditTopBar(
            isEditing = true,
            actionVisible = true,
            onBackClick = {},
            onEditClick = {},
            onApplyClick = {},
        )
    }
}

@Preview(name = "액션 숨김", showBackground = true)
@Composable
private fun DocumentEditTopBarActionHiddenPreview() {
    ModeraTheme {
        DocumentEditTopBar(
            isEditing = false,
            actionVisible = false,
            onBackClick = {},
            onEditClick = {},
            onApplyClick = {},
        )
    }
}