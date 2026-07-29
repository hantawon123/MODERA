package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.home.R

@Composable
internal fun RecentSearchSection(
    recentSearchTerms: List<String>,
    onRecentSearchClick: (String) -> Unit,
    onRecentSearchDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.home_recent_search_title),
            style = ModeraTheme.typography.captionSB12,
            color = ModeraTheme.colors.gray400,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        recentSearchTerms.forEach { term ->
            RecentSearchTermItem(
                text = term,
                onDeleteClick = { onRecentSearchDelete(term) },
                onItemClick = { onRecentSearchClick(term) },
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RecentSearchTermItem(
    text: String,
    onDeleteClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(
                width = 1.dp,
                color = ModeraTheme.colors.gray200,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(start = 10.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
            .clickable(enabled = true, onClick = onItemClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = ModeraTheme.typography.captionR12,
            color = ModeraTheme.colors.gray700,
        )

        Spacer(Modifier.width(2.dp))

        Icon(
            imageVector = ImageVector.vectorResource(ModeraIcons.Close),
            contentDescription = stringResource(R.string.home_search_term_item_close_button),
            tint = ModeraTheme.colors.gray500,
            modifier = Modifier
                .size(20.dp)
                .clickable(enabled = true, onClick = onDeleteClick),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun RecentSearchSectionPreview() {
    ModeraTheme {
        RecentSearchSection(
            recentSearchTerms = listOf(
                "성심당 케이크",
                "KTX 예약",
                "레시피",
            ),
            onRecentSearchClick = {},
            onRecentSearchDelete = {},
        )
    }
}
