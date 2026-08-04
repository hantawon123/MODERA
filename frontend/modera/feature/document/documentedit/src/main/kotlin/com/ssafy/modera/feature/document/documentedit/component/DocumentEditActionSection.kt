package com.ssafy.modera.feature.document.documentedit.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraIconTextButton
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.documentedit.R

@Composable
internal fun DocumentEditActionSection(
    onAddImagesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(
                R.string.document_edit_description,
            ),
            style = ModeraTheme.typography.captionR12,
            color = ModeraTheme.colors.gray500,
        )

        Spacer(modifier = Modifier.height(20.dp))

        ModeraIconTextButton(
            text = stringResource(
                R.string.document_edit_add_image,
            ),
            icon = painterResource(
                ModeraIcons.Add,
            ),
            onClick = onAddImagesClick,
            buttonColor = ModeraTheme.colors.white,
            contentColor = ModeraTheme.colors.gray500,
            borderColor = ModeraTheme.colors.gray400,
            isDashedBorder = true,
        )
    }
}

@Preview(name = "DocumentEditActionSection", showBackground = true)
@Composable
private fun DocumentEditActionSectionPreview() {
    ModeraTheme {
        DocumentEditActionSection(
            onAddImagesClick = {},
            modifier = Modifier.padding(
                horizontal = 24.dp,
                vertical = 16.dp,
            ),
        )
    }
}