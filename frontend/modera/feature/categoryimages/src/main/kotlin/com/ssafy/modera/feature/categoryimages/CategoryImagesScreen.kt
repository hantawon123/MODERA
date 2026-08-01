package com.ssafy.modera.feature.categoryimages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.ImageAnalysisStatus
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.categoryimages.component.AnalyzedImageItem
import com.ssafy.modera.feature.categoryimages.component.CategoryImagesTopAppBar
import com.ssafy.modera.feature.categoryimages.component.SelectedImagesDeleteBar

@Composable
fun CategoryImagesScreen(
    categoryName: String,
    onBackClick: () -> Unit,
    onImageClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryImagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is CategoryImagesUiState.Loading -> {
            LoadingScreen(
                modifier = modifier,
            )
        }

        is CategoryImagesUiState.Success -> {
            CategoryImagesScreen(
                categoryName = categoryName,
                images = state.images,
                onBackClick = onBackClick,
                onImageClick = onImageClick,
                onDeleteImages = {},
                modifier = modifier,
            )
        }

        is CategoryImagesUiState.Error -> {
            CategoryImagesErrorScreen(
                categoryName = categoryName,
                onBackClick = onBackClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun CategoryImagesScreen(
    categoryName: String,
    images: List<AnalyzedImage>,
    onBackClick: () -> Unit,
    onImageClick: (Long) -> Unit,
    onDeleteImages: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectionMode by remember {
        mutableStateOf(false)
    }

    val selectedImageIds = remember {
        mutableStateListOf<Long>()
    }

    fun clearSelection() {
        selectionMode = false
        selectedImageIds.clear()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.gray50),
    ) {
        CategoryImagesTopAppBar(
            categoryName = categoryName,
            selectionMode = selectionMode,
            onBackClick = {
                if (selectionMode) {
                    clearSelection()
                } else {
                    onBackClick()
                }
            },
            onSelectionClick = {
                if (selectionMode) {
                    clearSelection()
                } else {
                    selectionMode = true
                }
            },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
            if (images.isEmpty()) {
                CategoryImagesEmptyScreen(
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = if (selectionMode) {
                            88.dp
                        } else {
                            24.dp
                        },
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = images,
                        key = AnalyzedImage::id,
                    ) { image ->
                        AnalyzedImageItem(
                            analyzedImage = image,
                            selected = image.id in selectedImageIds,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    if (image.id in selectedImageIds) {
                                        selectedImageIds.remove(image.id)
                                    } else {
                                        selectedImageIds.add(image.id)
                                    }
                                } else {
                                    onImageClick(image.id)
                                }
                            },
                        )
                    }
                }
            }

            if (selectionMode) {
                SelectedImagesDeleteBar(
                    selectedCount = selectedImageIds.size,
                    enabled = selectedImageIds.isNotEmpty(),
                    onDeleteClick = {
                        onDeleteImages(selectedImageIds.toSet())
                        clearSelection()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun CategoryImagesErrorScreen(
    categoryName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.gray50),
    ) {
        CategoryImagesTopAppBar(
            categoryName = categoryName,
            selectionMode = false,
            onBackClick = onBackClick,
            onSelectionClick = {},
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "이미지 목록을 불러오지 못했습니다.",
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray700,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CategoryImagesEmptyScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(ModeraTheme.colors.gray50),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "등록된 이미지가 없습니다.",
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray700,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(
    name = "Category Image List",
    showBackground = true,
)
@Composable
private fun CategoryImagesScreenPreview() {
    ModeraTheme {
        CategoryImagesScreen(
            categoryName = "주식",
            images = previewAnalyzedImageSummaries,
            onBackClick = {},
            onImageClick = {},
            onDeleteImages = {},
        )
    }
}

@Preview(
    name = "Category Image Empty",
    showBackground = true,
)
@Composable
private fun CategoryImagesEmptyScreenPreview() {
    ModeraTheme {
        CategoryImagesScreen(
            categoryName = "주식",
            images = emptyList(),
            onBackClick = {},
            onImageClick = {},
            onDeleteImages = {},
        )
    }
}

@Preview(
    name = "Category Image Error",
    showBackground = true,
)
@Composable
private fun CategoryImagesErrorScreenPreview() {
    ModeraTheme {
        CategoryImagesErrorScreen(
            categoryName = "주식",
            onBackClick = {},
        )
    }
}

private val previewAnalyzedImageSummaries = List(12) { index ->
    AnalyzedImage(
        id = index.toLong(),
        title = "삼성전자 주가 전망 및 투자 분석",
        summary = "삼성전자 주가 전망 및 투자 분석",
        thumbnailUrl = "https://picsum.photos/seed/category_$index/400/600",
        hashtags = listOf(
            "주식",
            "삼성전자",
            "투자",
        ),
        favorite = index % 3 == 0,
    )
}