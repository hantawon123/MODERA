package com.ssafy.modera.core.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.core.ui.R

/**
 * 정렬 트리거 공통 컴포넌트.
 */
@Composable
fun <T> ModeraSortSection(
    selectedLabel: String,
    expanded: Boolean,
    options: List<T>,
    selectedOption: T,
    labelOf: (T) -> String,
    onSortClick: () -> Unit,
    onDismissRequest: () -> Unit,
    onOptionClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            SortButton(
                label = selectedLabel,
                onClick = onSortClick,
            )

            ModeraSortPopup(
                expanded = expanded,
                options = options,
                selectedOption = selectedOption,
                labelOf = labelOf,
                onDismissRequest = onDismissRequest,
                onOptionClick = onOptionClick,
            )
        }
    }
}

@Composable
private fun SortButton(
    label: String,
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
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray500,
            )

            Icon(
                painter = painterResource(ModeraIcons.ArrowDown),
                contentDescription = stringResource(R.string.sort_menu_content_description),
                tint = ModeraTheme.colors.gray500,
            )
        }
    }
}

@Preview(
    name = "Sort Section",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5,
    widthDp = 200,
)
@Composable
private fun ModeraSortSectionPreview() {
    ModeraTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ModeraTheme.colors.gray50)
                .padding(horizontal = 12.dp),
        ) {
            ModeraSortSection(
                selectedLabel = CategorySortType.NAME_ASC.label,
                expanded = false,
                options = CategorySortType.entries,
                selectedOption = CategorySortType.NAME_ASC,
                labelOf = { it.label },
                onSortClick = {},
                onDismissRequest = {},
                onOptionClick = {},
            )
        }
    }
}
