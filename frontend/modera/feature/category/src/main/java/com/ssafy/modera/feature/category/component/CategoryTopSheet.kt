package com.ssafy.modera.feature.category.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.category.CategorySheetItem
import com.ssafy.modera.feature.category.R

@Composable
fun CategoryTopSheet(
    visible: Boolean,
    categories: List<CategorySheetItem>,
    selectedCategory: String,
    onCategoryClick: (CategorySheetItem) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                bottomStart = 20.dp,
                                bottomEnd = 20.dp,
                            ),
                        )
                        .background(ModeraTheme.colors.white)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(bottom = 12.dp),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = CategoryTopSheetDefaults.MaxHeight),
                        contentPadding = PaddingValues(
                            horizontal = CategoryTopSheetDefaults.HorizontalPadding,
                            vertical = 8.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(
                            CategoryTopSheetDefaults.ColumnSpacing,
                        ),
                    ) {
                        itemsIndexed(
                            items = categories,
                            key = { index, _ -> index },
                        ) { _, category ->
                            CategoryTopSheetItem(
                                item = category,
                                selected = category.name == selectedCategory,
                                onClick = {
                                    onCategoryClick(category)
                                    onDismissRequest()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTopSheetItem(
    item: CategorySheetItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        ModeraTheme.colors.yellow800
    } else {
        ModeraTheme.colors.gray900
    }
    val countTextColor = if (selected) {
        ModeraTheme.colors.yellow800
    } else {
        ModeraTheme.colors.gray400
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = CategoryTopSheetDefaults.ItemVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f, fill = false),
                    style = if (selected) {
                        ModeraTheme.typography.bodySB14
                    } else {
                        ModeraTheme.typography.bodyR14
                    },
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (item.isNew) {
                    Spacer(modifier = Modifier.width(6.dp))
                    NewBadge()
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.count.toString(),
                style = ModeraTheme.typography.bodyR14,
                color = countTextColor,
                maxLines = 1,
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = ModeraTheme.colors.gray200,
        )
    }
}

@Composable
private fun NewBadge(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(ModeraTheme.colors.yellow900Bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.category_new_badge),
            style = ModeraTheme.typography.captionSB10,
            color = ModeraTheme.colors.white,
        )
    }
}

private object CategoryTopSheetDefaults {
    val MaxHeight = 420.dp
    val HorizontalPadding = 20.dp
    val ColumnSpacing = 24.dp
    val ItemVerticalPadding = 14.dp
}

@Preview(showBackground = true, widthDp = 360, heightDp = 520)
@Composable
private fun CategoryTopSheetPreview() {
    ModeraTheme {
        CategoryTopSheet(
            visible = true,
            categories = listOf(
                CategorySheetItem("기사", 123),
                CategorySheetItem("기사asdfasdfasdfasdf", 123, isNew = true),
                CategorySheetItem("스포츠", 123),
                CategorySheetItem("스포츠", 123, isNew = true),
                CategorySheetItem("뉴스", 123),
                CategorySheetItem("뉴스", 123),
                CategorySheetItem("예약", 123),
                CategorySheetItem("예약", 123),
                CategorySheetItem("음식", 123),
                CategorySheetItem("음식", 123),
                CategorySheetItem("일정", 123),
                CategorySheetItem("예약", 123),
                CategorySheetItem("쇼핑", 123),
                CategorySheetItem("음식", 123),
                CategorySheetItem("음식", 1),
            ),
            selectedCategory = "쇼핑",
            onCategoryClick = {},
            onDismissRequest = {},
        )
    }
}
