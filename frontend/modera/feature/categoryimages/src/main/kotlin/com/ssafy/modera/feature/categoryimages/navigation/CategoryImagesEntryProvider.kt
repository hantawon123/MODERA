package com.ssafy.modera.feature.categoryimages.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.categoryimages.CategoryImagesScreen
import com.ssafy.modera.feature.categoryimages.CategoryImagesViewModel
import com.ssafy.modera.feature.categoryimages.CategoryImagesViewModel.Factory

fun EntryProviderScope<NavKey>.categoryImagesEntry(
    navigator: Navigator,
) {
    entry<CategoryImagesNavKey> { navKey ->
        val categoryName = navKey.category.title
        val categoryId = navKey.category.id

        CategoryImagesScreen(
            categoryName = categoryName,
            onBackClick = navigator::goBack,
            onImageClick = {},
            viewModel = hiltViewModel<CategoryImagesViewModel, Factory>(
                key = categoryId.toString(),
            ) { factory ->
                factory.create(categoryId)
            },
        )
    }
}
