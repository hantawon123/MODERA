package com.ssafy.modera.feature.document.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraSortSection
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.document.DocumentSortType
import com.ssafy.modera.feature.document.R

@Composable
internal fun DocumentListHeader(
    documentCount: Int,
    sortType: DocumentSortType,
    onSortTypeChange: (DocumentSortType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSortExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val sortLabels = DocumentSortType.entries.associateWith { type ->
        stringResource(type.labelRes)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(
            text = stringResource(
                R.string.document_total_count,
                documentCount,
            ),
            modifier = Modifier.align(Alignment.CenterStart),
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray500,
        )

        ModeraSortSection(
            selectedLabel = sortLabels.getValue(sortType),
            expanded = isSortExpanded,
            options = DocumentSortType.entries,
            selectedOption = sortType,
            labelOf = sortLabels::getValue,
            onSortClick = {
                isSortExpanded = !isSortExpanded
            },
            onDismissRequest = {
                isSortExpanded = false
            },
            onOptionClick = { selectedSortType ->
                if (selectedSortType != sortType) {
                    onSortTypeChange(selectedSortType)
                }
            },
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@Preview(
    name = "Document List Header",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 390,
)
@Composable
private fun DocumentListHeaderPreview() {
    ModeraTheme {
        DocumentListHeader(
            documentCount = 12,
            sortType = DocumentSortType.LATEST,
            onSortTypeChange = {},
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}