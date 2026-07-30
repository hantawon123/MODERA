package com.ssafy.modera.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraSortSection
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.component.ModeraTopBarDefaults
import com.ssafy.modera.core.component.item.ModeraMaterialItem
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.category.CategorySheetItem
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.core.util.statusBarTopPadding
import com.ssafy.modera.feature.category.component.CategoryTopSheet

@Composable
fun CategoryRoute(
    onItemClick: (Long) -> Unit,
    onSearchIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by rememberSaveable { mutableStateOf("쇼핑") }
    var selectedSortType by rememberSaveable {
        mutableStateOf(CategorySortType.UPDATED_AT_ASC)
    }
    var showCategorySheet by rememberSaveable { mutableStateOf(false) }
    var showSortPopup by rememberSaveable { mutableStateOf(false) }

    val categories = remember { /* TODO: viewModel로 이동 */
        listOf(
            CategorySheetItem(1,"기사", 123),
            CategorySheetItem(1,"기사", 123, isNew = true),
            CategorySheetItem(1,"스포츠", 123),
            CategorySheetItem(1,"스포츠", 123, isNew = true),
            CategorySheetItem(1,"뉴스", 123),
            CategorySheetItem(1,"뉴스", 123),
            CategorySheetItem(1,"예약", 123),
            CategorySheetItem(1,"예약", 123),
            CategorySheetItem(1,"음식", 123),
            CategorySheetItem(1,"음식", 123),
            CategorySheetItem(1,"일정", 123),
            CategorySheetItem(1,"예약", 123),
            CategorySheetItem(1,"쇼핑", 1232),
            CategorySheetItem(1,"음식", 123),
            CategorySheetItem(1,"음식", 1),
        )
    }
    val analyzedImages = remember {
        List(16) { index ->
            AnalyzedImage(
                id = (index + 1).toLong(),
                title = "성심당 케이크 리스트",
                summary = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
                hashtags = listOf("기차", "예약", "KTX"),
                thumbnailUrl = "",
            )
        }
    }

    CategoryScreen(
        selectedCategory = selectedCategory,
        categories = categories,
        analyzedImages = analyzedImages,
        selectedSortType = selectedSortType,
        showCategorySheet = showCategorySheet,
        showSortPopup = showSortPopup,
        onSearchIconClick = onSearchIconClick,
        onCategoryTitleClick = { showCategorySheet = true },
        onCategorySheetDismiss = { showCategorySheet = false },
        onCategorySelect = { selectedCategory = it },
        onSortClick = { showSortPopup = true },
        onSortPopupDismiss = { showSortPopup = false },
        onSortTypeSelect = {
            selectedSortType = it
            showSortPopup = false
        },
        onItemClick = onItemClick,
        modifier = modifier,
    )
}

@Composable
fun CategoryScreen(
    selectedCategory: String,
    categories: List<CategorySheetItem>,
    analyzedImages: List<AnalyzedImage>,
    selectedSortType: CategorySortType,
    showCategorySheet: Boolean,
    showSortPopup: Boolean,
    onSearchIconClick: () -> Unit,
    onCategoryTitleClick: () -> Unit,
    onCategorySheetDismiss: () -> Unit,
    onCategorySelect: (String) -> Unit,
    onSortClick: () -> Unit,
    onSortPopupDismiss: () -> Unit,
    onSortTypeSelect: (CategorySortType) -> Unit,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarTopPadding(),
        ) {
            ModeraTopBar(
                onBackClick = {},
                leftContent = {
                    Row(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable(onClick = onCategoryTitleClick),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selectedCategory,
                            style = ModeraTheme.typography.titleSB20,
                            color = ModeraTheme.colors.gray900,
                        )
                        Icon(
                            painter = painterResource(ModeraIcons.ArrowDown),
                            contentDescription = stringResource(
                                R.string.category_title_picker_description,
                            ),
                            tint = ModeraTheme.colors.gray700,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(16.dp),
                        )
                    }
                },
                rightContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(ModeraIcons.Search),
                        contentDescription = stringResource(R.string.icon_search_description),
                        tint = ModeraTheme.colors.gray700,
                        modifier = Modifier
                            .size(ModeraTopBarDefaults.IconSize)
                            .clickable(onClick = onSearchIconClick),
                    )
                },
            )

            Box() {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = CategoryScreenDefaults.HorizontalPadding),
                ) {
                    item {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 8.dp),
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.category_item_count,
                                        analyzedImages.size,
                                    ),
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    style = ModeraTheme.typography.bodyR14,
                                    color = ModeraTheme.colors.gray500,
                                )

                                ModeraSortSection(
                                    selectedLabel = selectedSortType.label,
                                    expanded = showSortPopup,
                                    options = CategorySortType.entries,
                                    selectedOption = selectedSortType,
                                    labelOf = { it.label },
                                    onSortClick = onSortClick,
                                    onDismissRequest = onSortPopupDismiss,
                                    onOptionClick = onSortTypeSelect,
                                )
                            }

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = ModeraTheme.colors.gray200,
                            )
                        }
                    }

                    items(
                        items = analyzedImages,
                        key = { it.id },
                    ) { analyzedImage ->
                        ModeraMaterialItem(
                            title = analyzedImage.title,
                            description = analyzedImage.summary,
                            tags = analyzedImage.hashtags,
                            imageUrl = analyzedImage.thumbnailUrl,
                            onClick = { onItemClick(analyzedImage.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                CategoryTopSheet(
                    visible = showCategorySheet,
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategoryClick = { onCategorySelect(it.title) },
                    onDismissRequest = onCategorySheetDismiss,
                )
            }
        }
    }
}

internal object CategoryScreenDefaults {
    val HorizontalPadding = 20.dp
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CategoryScreenPreview() {
    ModeraTheme {
        CategoryScreen(
            selectedCategory = "쇼핑",
            categories = listOf(
                CategorySheetItem(1,"쇼핑", 123),
                CategorySheetItem(1,"음식", 1),
                CategorySheetItem(1,"여행", 10, isNew = true),
            ),
            analyzedImages = listOf(
                AnalyzedImage(
                    id = 1.toLong(),
                    title = "성심당 케이크 리스트",
                    summary = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
                    hashtags = listOf("기차", "예약", "KTX"),
                    thumbnailUrl = "",
                ),
            ),
            selectedSortType = CategorySortType.UPDATED_AT_ASC,
            showCategorySheet = false,
            showSortPopup = false,
            onSearchIconClick = {},
            onCategoryTitleClick = {},
            onCategorySheetDismiss = {},
            onCategorySelect = {},
            onSortClick = {},
            onSortPopupDismiss = {},
            onSortTypeSelect = {},
            onItemClick = {},
        )
    }
}
