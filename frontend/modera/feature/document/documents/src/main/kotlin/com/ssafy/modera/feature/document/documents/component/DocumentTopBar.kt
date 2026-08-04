package com.ssafy.modera.feature.document.documents.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.document.R
import com.ssafy.modera.core.designsystem.R as designR

@Composable
internal fun DocumentTopBar(
    modifier: Modifier = Modifier,
) {
    ModeraTopBar(
        onBackClick = {},
        modifier = modifier.padding(start = 14.dp),
        leftContent = {
            Text(
                text = stringResource(R.string.document_title),
                style = ModeraTheme.typography.titleB22,
                color = ModeraTheme.colors.gray900,
                maxLines = 1,
            )
        },
        rightContent = {
            Image(
                painter = painterResource(designR.drawable.img_modera_logo),
                contentDescription = stringResource(designR.string.logo_content_description),
                modifier = Modifier.width(96.dp)
            )
        },
    )
}

@Preview(name = "Document Top Bar", showBackground = true)
@Composable
private fun DocumentTopBarPreview() {
    ModeraTheme {
        DocumentTopBar()
    }
}