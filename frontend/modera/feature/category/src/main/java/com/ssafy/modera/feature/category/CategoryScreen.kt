package com.ssafy.modera.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.component.ModeraScrollToTopButton
import com.ssafy.modera.core.component.ModeraSearchBar
import com.ssafy.modera.core.component.ModeraSortSection
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.component.SearchBarMode
import com.ssafy.modera.core.component.item.ModeraAnalyzedImageItem
import com.ssafy.modera.core.component.rememberShowScrollToTop
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.LoadingWheel
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSortType
import com.ssafy.modera.core.model.category.CategorySheetItem
import com.ssafy.modera.core.ui.EmptyScreen
import com.ssafy.modera.core.ui.ErrorScreen
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.category.component.CategoryTopSheet
import kotlinx.coroutines.launch

@Composable
fun CategoryRoute(
    onItemClick: (Long) -> Unit,
    selectedCategoryId: Long? = null,
    modifier: Modifier = Modifier,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(selectedCategoryId) {
        viewModel.initialize(selectedCategoryId)
    }

    when (val state = uiState) {
        CategoryUiState.Loading -> {
            LoadingScreen(
                modifier = modifier,
            )
        }

        CategoryUiState.Error -> {
            ErrorScreen(
                message = stringResource(R.string.category_load_error),
                modifier = modifier,
            )
        }

        is CategoryUiState.Success -> {
            CategoryScreen(
                selectedCategoryId = state.selectedCategoryId,
                selectedCategoryTitle = state.selectedCategoryTitle,
                isAllCategorySelected = state.isAllCategorySelected,
                categories = state.categories,
                analyzedImages = state.analyzedImages,
                totalImageCount = state.totalImageCount,
                selectedSortType = state.selectedSortType,
                searchQuery = state.searchQuery,
                showCategorySheet = state.showCategorySheet,
                showSortPopup = state.showSortPopup,
                isLoadingMore = state.isLoadingMore,
                hasNextPage = state.hasNextPage,
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                onCategoryTitleClick = viewModel::onCategoryTitleClick,
                onCategorySheetDismiss = viewModel::onCategorySheetDismiss,
                onCategorySelect = viewModel::onCategorySelect,
                onSortClick = viewModel::onSortClick,
                onSortPopupDismiss = viewModel::onSortPopupDismiss,
                onSortTypeSelect = viewModel::onSortTypeSelect,
                onLoadMore = viewModel::loadNextPage,
                onItemClick = onItemClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun CategoryScreen(
    selectedCategoryId: Long,
    selectedCategoryTitle: String,
    isAllCategorySelected: Boolean,
    categories: List<CategorySheetItem>,
    analyzedImages: List<AnalyzedImage>,
    totalImageCount: Long,
    selectedSortType: AnalyzedImageSortType,
    searchQuery: String,
    showCategorySheet: Boolean,
    showSortPopup: Boolean,
    isLoadingMore: Boolean,
    hasNextPage: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCategoryTitleClick: () -> Unit,
    onCategorySheetDismiss: () -> Unit,
    onCategorySelect: (Long) -> Unit,
    onSortClick: () -> Unit,
    onSortPopupDismiss: () -> Unit,
    onSortTypeSelect: (AnalyzedImageSortType) -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val showScrollToTop = rememberShowScrollToTop(listState)
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val displayCategoryTitle = if (isAllCategorySelected) {
        stringResource(R.string.category_all)
    } else {
        selectedCategoryTitle
    }
    val isSearching = searchQuery.isNotBlank()
    val displayImageCount = if (totalImageCount > 0) {
        totalImageCount
    } else {
        analyzedImages.size.toLong()
    }

    LaunchedEffect(listState, hasNextPage, isLoadingMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItemIndex >= totalItems - CategoryScreenDefaults.LOAD_MORE_THRESHOLD
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && hasNextPage && !isLoadingMore) {
                onLoadMore()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                },
            ) {
                ModeraTopBar(
                    onBackClick = {},
                    leftContent = {
                        Row(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clickable(onClick = onCategoryTitleClick),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = displayCategoryTitle,
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
                )
            }

            ModeraSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = stringResource(R.string.category_search_placeholder),
                mode = SearchBarMode.General,
                modifier = Modifier
                    .padding(horizontal = CategoryScreenDefaults.HorizontalPadding),
            )

            Box {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = CategoryScreenDefaults.HorizontalPadding)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                focusManager.clearFocus()
                            }
                        },
                ) {
                    item {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 8.dp),
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.category_item_count,
                                        displayImageCount,
                                    ),
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    style = ModeraTheme.typography.bodyR14,
                                    color = ModeraTheme.colors.gray500,
                                )

                                ModeraSortSection(
                                    selectedLabel = selectedSortType.label,
                                    expanded = showSortPopup,
                                    options = AnalyzedImageSortType.entries,
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

                    if (isSearching && analyzedImages.isEmpty()) {
                        item(key = "search_empty") {
                            EmptyScreen(
                                message = stringResource(R.string.category_search_result_empty),
                                modifier = Modifier.padding(top = 40.dp),
                            )
                        }
                    } else {
                        items(
                            items = analyzedImages,
                            key = { it.id },
                        ) { analyzedImage ->
                            ModeraAnalyzedImageItem(
                                title = analyzedImage.title,
                                description = analyzedImage.summary,
                                tags = analyzedImage.hashtags,
                                imageUrl = analyzedImage.thumbnailUrl,
                                favorite = analyzedImage.favorite,
                                isDocumented = analyzedImage.isDocumented,
                                hasSchedule = analyzedImage.hasSchedule,
                                onClick = { onItemClick(analyzedImage.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingWheel(
                                    contentDescription = stringResource(
                                        R.string.category_loading_more_description,
                                    ),
                                )
                            }
                        }
                    }
                }

                CategoryTopSheet(
                    visible = showCategorySheet,
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategoryClick = { onCategorySelect(it.id) },
                    onDismissRequest = onCategorySheetDismiss,
                )

                ModeraScrollToTopButton(
                    visible = showScrollToTop,
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

internal object CategoryScreenDefaults {
    val HorizontalPadding = 20.dp
    const val LOAD_MORE_THRESHOLD = 3
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CategoryScreenPreview() {
    ModeraTheme {
        CategoryScreen(
            selectedCategoryId = CategorySheetItem.ALL_CATEGORY_ID,
            selectedCategoryTitle = "",
            isAllCategorySelected = true,
            categories = listOf(
                CategorySheetItem(
                    id = CategorySheetItem.ALL_CATEGORY_ID,
                    title = "",
                    itemCount = 134,
                    isAll = true,
                ),
                CategorySheetItem(1, "쇼핑", 123),
                CategorySheetItem(2, "음식", 1),
                CategorySheetItem(3, "여행", 10, isNew = true),
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
            totalImageCount = 1,
            selectedSortType = AnalyzedImageSortType.UPLOADED_DESC,
            searchQuery = "",
            showCategorySheet = false,
            showSortPopup = false,
            isLoadingMore = false,
            hasNextPage = false,
            onSearchQueryChange = {},
            onCategoryTitleClick = {},
            onCategorySheetDismiss = {},
            onCategorySelect = {},
            onSortClick = {},
            onSortPopupDismiss = {},
            onSortTypeSelect = {},
            onLoadMore = {},
            onItemClick = {},
        )
    }
}
