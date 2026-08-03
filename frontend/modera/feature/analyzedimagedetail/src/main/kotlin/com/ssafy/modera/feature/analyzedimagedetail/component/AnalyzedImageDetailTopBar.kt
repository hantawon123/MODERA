package com.ssafy.modera.feature.analyzedimagedetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraDropdownMenu
import com.ssafy.modera.core.component.ModeraDropdownMenuItem
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.component.ModeraTopBarDefaults
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.analyzedimagedetail.R

@Composable
internal fun AnalyzedImageDetailTopBar(
    title: String,
    menuExpanded: Boolean,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onDocumentClick: () -> Unit,
    onReanalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val menuOffset = with(LocalDensity.current) {
        IntOffset(
            x = 0,
            y = 36.dp.roundToPx(),
        )
    }

    val menuItems = listOf(
        ModeraDropdownMenuItem(
            icon = ModeraIcons.FileDocument,
            label = stringResource(
                R.string.analyzed_image_detail_create_document,
            ),
            onClick = onDocumentClick,
        ),
        ModeraDropdownMenuItem(
            icon = ModeraIcons.Refresh,
            label = stringResource(
                R.string.analyzed_image_detail_reanalyze,
            ),
            onClick = onReanalyzeClick,
        ),
        ModeraDropdownMenuItem(
            icon = ModeraIcons.Trash,
            label = stringResource(
                R.string.analyzed_image_detail_delete,
            ),
            contentColor = ModeraTheme.colors.red,
            onClick = onDeleteClick,
        ),
    )

    ModeraTopBar(
        onBackClick = onBackClick,
        modifier = modifier,
        centerContent = {
            Text(
                text = title,
                style = ModeraTheme.typography.bodySB16,
                color = ModeraTheme.colors.gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 36.dp)
            )
        },
        rightContent = {
            Box {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        ModeraIcons.MoreVertical,
                    ),
                    contentDescription = stringResource(
                        R.string.analyzed_image_detail_more,
                    ),
                    modifier = Modifier
                        .size(ModeraTopBarDefaults.IconSize)
                        .clickable(onClick = onMoreClick),
                    tint = ModeraTheme.colors.gray700,
                )

                ModeraDropdownMenu(
                    expanded = menuExpanded,
                    items = menuItems,
                    onDismissRequest = onDismissMenu,
                    offset = menuOffset,
                )
            }
        },
    )
}

@Preview(
    name = "AnalyzedImageDetailTopBar",
    showBackground = true,
)
@Composable
private fun AnalyzedImageDetailTopBarPreview() {
    ModeraTheme {
        AnalyzedImageDetailTopBar(
            title = "",
            menuExpanded = false,
            onBackClick = {},
            onMoreClick = {},
            onDismissMenu = {},
            onDocumentClick = {},
            onReanalyzeClick = {},
            onDeleteClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(ModeraTheme.colors.white),
        )
    }
}

@Preview(
    name = "AnalyzedImageDetailTopBar - Menu Expanded",
    showBackground = true,
    widthDp = 360,
    heightDp = 240,
)
@Composable
private fun AnalyzedImageDetailTopBarMenuPreview() {
    ModeraTheme {
        AnalyzedImageDetailTopBar(
            title = "",
            menuExpanded = true,
            onBackClick = {},
            onMoreClick = {},
            onDismissMenu = {},
            onDocumentClick = {},
            onReanalyzeClick = {},
            onDeleteClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(ModeraTheme.colors.white),
        )
    }
}