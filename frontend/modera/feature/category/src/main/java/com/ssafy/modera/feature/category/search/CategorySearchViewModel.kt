package com.ssafy.modera.feature.category.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.feature.category.CategoryMaterialUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategorySearchViewModel @Inject constructor() : ViewModel() {

    private val allMaterials = CategorySearchDummyData.allMaterials

    private val _uiState = MutableStateFlow<CategorySearchUiState>(CategorySearchUiState.Loading)
    val uiState: StateFlow<CategorySearchUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun onSearchQueryChanged(query: String) {
        updateSuccessState { state ->
            state.copy(
                searchQuery = query,
                searchResults = filterMaterials(query, allMaterials),
            )
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = CategorySearchUiState.Success(
                    recentMaterials = CategorySearchDummyData.recentMaterials,
                )
            } catch (exception: Exception) {
                _uiState.value = CategorySearchUiState.Error(exception)
            }
        }
    }

    private inline fun updateSuccessState(
        update: (CategorySearchUiState.Success) -> CategorySearchUiState.Success,
    ) {
        val current = _uiState.value
        if (current is CategorySearchUiState.Success) {
            _uiState.update { update(current) }
        }
    }

    private fun filterMaterials(
        query: String,
        materials: List<AnalyzedImage>,
    ): List<AnalyzedImage> {
        if (query.isBlank()) return emptyList()

        return materials.filter { material ->
            material.title.contains(query, ignoreCase = true) ||
                material.summary.contains(query, ignoreCase = true) ||
                material.hashtags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
    }
}
