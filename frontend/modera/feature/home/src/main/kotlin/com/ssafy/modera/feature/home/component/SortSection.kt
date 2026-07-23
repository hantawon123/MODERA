package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.feature.home.R
import com.ssafy.modera.feature.home.label

@Composable
internal fun SortSection(
    selectedSortType: CategorySortType,
    onSortClick: () -> Unit,
    onPositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        SortButton(
            sortType = selectedSortType,
            onClick = onSortClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .onGloballyPositioned { coordinates ->
                    onPositioned(coordinates.boundsInWindow())
                },
        )
    }
}

@Composable
private fun SortButton(
    sortType: CategorySortType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ClickableSurface(
        onClick = onClick,
        modifier = modifier,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sortType.label,
                style = ModeraTheme.typography.body2,
                color = ModeraTheme.colors.typo,
            )

            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                contentDescription = stringResource(R.string.home_sort_menu_content_description)
            )
        }
    }
}

@Preview(
    name = "Sort Section",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5,
    widthDp = 200
)
@Composable
private fun SortSectionPreview() {
    ModeraTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ModeraTheme.colors.gray)
                .padding(horizontal = 12.dp),
        ) {
            SortSection(
                selectedSortType = CategorySortType.NAME_ASC,
                onSortClick = {},
                onPositioned = {},
            )
        }
    }
}