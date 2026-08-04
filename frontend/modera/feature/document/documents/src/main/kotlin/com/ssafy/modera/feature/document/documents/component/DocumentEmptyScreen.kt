package com.ssafy.modera.feature.document.documents.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.document.R
import com.ssafy.modera.core.ui.R as uiR

@Composable
internal fun DocumentEmptyScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(uiR.drawable.img_chracter_error),
                contentDescription = null,
                modifier = Modifier.size(124.dp),
            )

            Spacer(
                modifier = Modifier.height(24.dp),
            )

            Text(
                text = stringResource(R.string.document_empty_title),
                style = ModeraTheme.typography.titleB22,
                color = ModeraTheme.colors.gray700,
                maxLines = 1,
            )

            Spacer(
                modifier = Modifier.height(16.dp),
            )

            Text(
                text = stringResource(R.string.document_empty_description),
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray400,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "Document Screen - Empty", showBackground = true)
@Composable
private fun DocumentEmptyScreenPreview() {
    ModeraTheme {
        DocumentEmptyScreen()
    }
}