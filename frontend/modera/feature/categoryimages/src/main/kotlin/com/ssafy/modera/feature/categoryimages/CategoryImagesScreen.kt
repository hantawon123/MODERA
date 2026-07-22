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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.AnalyzedImageSummary
import com.ssafy.modera.feature.categoryimages.component.AnalyzedImageItem
import com.ssafy.modera.feature.categoryimages.component.CategoryImagesTopAppBar
import com.ssafy.modera.feature.categoryimages.component.SelectedImagesDeleteBar

@Composable
fun CategoryImagesScreen(
    categoryName: String,
    images: List<AnalyzedImageSummary>,
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
            .background(ModeraTheme.colors.gray),
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = if (selectionMode) 88.dp else 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = images,
                    key = AnalyzedImageSummary::id,
                ) { image ->
                    AnalyzedImageItem(
                        analyzedImageSummary = image,
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

private val previewAnalyzedImageSummaries = List(12) { index ->
    AnalyzedImageSummary(
        id = index.toLong(),
        title = "삼성전자 주가 전망 및 투자 분석",
        imageUrl = "https://picsum.photos/seed/category_$index/400/600",
        hashtags = listOf(
            "주식",
            "삼성전자",
            "투자",
        ),
    )
}