package com.ssafy.modera.feature.categoryimages.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.categoryimages.R
import com.ssafy.modera.core.designsystem.R as designSystemR

@Composable
internal fun CategoryImagesTopAppBar(
    categoryName: String,
    selectionMode: Boolean,
    onBackClick: () -> Unit,
    onSelectionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = categoryName,
        onBackClick = onBackClick,
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top
            ),
        ),
    ) {
        ClickableSurface(
            onClick = onSelectionClick,
            color = Color.Transparent,
        ) {
            Text(
                text = stringResource(
                    if (selectionMode) {
                        R.string.category_image_list_cancel
                    } else {
                        R.string.category_image_list_select
                    },
                ),
                modifier = Modifier.padding(
                    horizontal = 4.dp,
                    vertical = 6.dp,
                ),
                style = ModeraTheme.typography.bodySB14,
                color = ModeraTheme.colors.blue,
            )
        }
    }
}

// Todo: 추후 공용 UI로 이동
@Composable
private fun TopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClickableSurface(
            onClick = onBackClick,
            color = Color.Transparent,
        ) {
            Icon(
                painter = painterResource(designSystemR.drawable.ic_arrow_left),
                contentDescription = stringResource(
                    R.string.top_app_bar_back,
                ),
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            style = ModeraTheme.typography.titleB22,
            color = ModeraTheme.colors.typo,
            maxLines = 1,
        )

        actions()
    }
}


@Preview(
    name = "Category Images TopAppBar - Default",
    showBackground = true,
)
@Composable
private fun CategoryImagesTopAppBarDefaultPreview() {
    ModeraTheme {
        CategoryImagesTopAppBar(
            categoryName = "카테고리",
            selectionMode = false,
            onBackClick = {},
            onSelectionClick = {},
        )
    }
}

@Preview(
    name = "Category Images TopAppBar - Selection Mode",
    showBackground = true,
)
@Composable
private fun CategoryImagesTopAppBarSelectionModePreview() {
    ModeraTheme {
        CategoryImagesTopAppBar(
            categoryName = "카테고리",
            selectionMode = true,
            onBackClick = {},
            onSelectionClick = {},
        )
    }
}