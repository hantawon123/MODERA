package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.feature.home.R
import com.ssafy.modera.feature.home.label
import kotlin.math.roundToInt

@Composable
internal fun CategorySortPopup(
    anchorBounds: Rect,
    selectedSortType: CategorySortType,
    onDismissRequest: () -> Unit,
    onSortTypeClick: (CategorySortType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val popupGap = 8.dp

    val popupOffset = with(density) {
        IntOffset(
            x = 0,
            y = (anchorBounds.bottom + popupGap.toPx()).roundToInt(),
        )
    }

    Popup(
        alignment = Alignment.TopEnd,
        offset = popupOffset,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        CategorySortPopupContent(
            selectedSortType = selectedSortType,
            onSortTypeClick = onSortTypeClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun CategorySortPopupContent(
    selectedSortType: CategorySortType,
    onSortTypeClick: (CategorySortType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false,
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(vertical = 6.dp),
    ) {
        CategorySortType.entries.forEachIndexed { index, sortType ->
            SortMenuItem(
                sortType = sortType,
                selected = sortType == selectedSortType,
                onClick = {
                    onSortTypeClick(sortType)
                },
                modifier = Modifier.padding(vertical = 12.dp),
            )

            if (index != CategorySortType.entries.lastIndex) {
                HorizontalDivider(
                    color = ModeraTheme.colors.gray,
                )
            }
        }
    }
}

@Composable
private fun SortMenuItem(
    sortType: CategorySortType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixedFontSize = with(LocalDensity.current) {
        14.dp.toSp()
    }

    val fixedLineHeight = with(LocalDensity.current) {
        20.dp.toSp()
    }

    val textStyle = if (selected) {
        ModeraTheme.typography.bodySB14
    } else {
        ModeraTheme.typography.bodyR14
    }

    val contentColor = if (selected) {
        ModeraTheme.colors.blue
    } else {
        ModeraTheme.colors.typo
    }

    ClickableSurface(
        onClick = onClick,
        modifier = modifier,
        color = Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sortType.label,
                style = textStyle.copy(
                    fontSize = fixedFontSize,
                    lineHeight = fixedLineHeight,
                ),
                color = contentColor,
            )

            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = if (selected) {
                    ModeraTheme.colors.blue
                } else {
                    Color.Transparent
                },
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(
    name = "Category Sort Popup Content - Name",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5,
)
@Composable
private fun CategorySortPopupNamePreview() {
    ModeraTheme {
        Box(
            modifier = Modifier.padding(24.dp),
        ) {
            CategorySortPopupContent(
                selectedSortType = CategorySortType.NAME_ASC,
                onSortTypeClick = {},
            )
        }
    }
}

@Preview(
    name = "Category Sort Popup Content - Latest",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5,
)
@Composable
private fun CategorySortPopupLatestPreview() {
    ModeraTheme {
        Box(
            modifier = Modifier.padding(24.dp),
        ) {
            CategorySortPopupContent(
                selectedSortType = CategorySortType.UPDATED_AT_DESC,
                onSortTypeClick = {},
            )
        }
    }
}

@Preview(
    name = "Category Sort Popup Content - Image Count",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5,
)
@Composable
private fun CategorySortPopupImageCountPreview() {
    ModeraTheme {
        Box(
            modifier = Modifier.padding(24.dp),
        ) {
            CategorySortPopupContent(
                selectedSortType = CategorySortType.IMAGE_COUNT_DESC,
                onSortTypeClick = {},
            )
        }
    }
}

@Preview(
    name = "Category Sort Popup - Actual Popup",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
    backgroundColor = 0xFFF5F5F5,
)
@Composable
private fun CategorySortPopupPreview() {
    ModeraTheme {
        CategorySortPopup(
            anchorBounds = Rect(
                left = 280f,
                top = 100f,
                right = 400f,
                bottom = 140f,
            ),
            selectedSortType = CategorySortType.NAME_ASC,
            onDismissRequest = {},
            onSortTypeClick = {},
        )
    }
}