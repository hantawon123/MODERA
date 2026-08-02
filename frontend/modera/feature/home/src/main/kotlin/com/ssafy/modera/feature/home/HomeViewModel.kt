package com.ssafy.modera.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.data.repository.search.RecentSearchRepository
import com.ssafy.modera.core.data.repository.search.SearchRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.feature.home.HomeScreenDefaults
import com.ssafy.modera.feature.home.state.HomeSearchState
import com.ssafy.modera.feature.home.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val searchRepository: SearchRepository,
    private val recentSearchRepository: RecentSearchRepository,
) : ViewModel() {
    private val searchState = MutableStateFlow(HomeSearchState())

    init {
        viewModelScope.launch {
            categoryRepository.refreshCategoriesIfEmpty()
        }
    }

    val uiState: StateFlow<HomeUiState> =
        combine(
            categoryRepository
                .observeCategories()
                .map { categories ->
                    categories
                        .filter { category -> category.itemCount > 0 }
                        .take(HomeScreenDefaults.MaxCategoryCount)
                }
                .asResult(),
            searchState,
            recentSearchRepository.recentSearchQueries,
        ) { result, search, recentSearchQueries ->
            when (result) {
                is Result.Success -> {
                    HomeUiState.Success(
                        categories = result.data,
                        searchQuery = search.searchQuery,
                        isSearchActive = search.isSearchActive,
                        recentSearchQueries = recentSearchQueries,
                        searchResults = search.searchResults,
                        isShowingSearchResults = search.isShowingSearchResults,
                        isSearchLoading = search.isSearchLoading,
                    )
                }

                is Result.Error -> {
                    HomeUiState.Error(
                        exception = result.exception,
                    )
                }

                Result.Loading -> {
                    HomeUiState.Loading
                }
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState.Success(
                    categories = emptyList(),
                    searchQuery = "",
                    isSearchActive = false,
                    recentSearchQueries = emptyList(),
                    searchResults = emptyList(),
                    isShowingSearchResults = false,
                    isSearchLoading = false,
                ),
            )

    fun onSearchQueryChanged(query: String) {
        searchState.update { state ->
            if (state.isShowingSearchResults) {
                state.copy(
                    searchQuery = query,
                    isSearchActive = true,
                    isShowingSearchResults = false,
                    searchResults = emptyList(),
                    isSearchLoading = false,
                )
            } else {
                state.copy(
                    searchQuery = query,
                    isSearchActive = true,
                )
            }
        }
    }

    fun onSearchBarFocusChanged(isFocused: Boolean) {
        if (isFocused) {
            searchState.update { state ->
                state.copy(
                    isSearchBarFocused = true,
                    isSearchActive = true,
                )
            }
            return
        }

        searchState.update { state ->
            state.copy(isSearchBarFocused = false)
        }

        viewModelScope.launch {
            yield()
            searchState.update { state ->
                state.deactivateIfIdle()
            }
        }
    }

    fun submitSearch() {
        val trimmedQuery = searchState.value.searchQuery.trim()
        if (trimmedQuery.isEmpty()) return

        searchState.update { state ->
            state.copy(
                searchQuery = trimmedQuery,
                isSearchActive = true,
                isShowingSearchResults = true,
                isSearchLoading = true,
                searchResults = emptyList(),
            )
        }

        viewModelScope.launch {
            recentSearchRepository.addRecentSearchQuery(trimmedQuery)

            val results = fetchSearchResults(trimmedQuery)
            searchState.update { state ->
                if (!state.isShowingSearchResults || state.searchQuery != trimmedQuery) {
                    return@update state
                }
                state.copy(
                    searchResults = results,
                    isSearchLoading = false,
                )
            }
        }
    }

    fun selectRecentSearchQuery(query: String) {
        searchState.update { state ->
            state.copy(
                searchQuery = query,
                isSearchActive = true,
            )
        }
        submitSearch()
    }

    fun removeRecentSearchQuery(query: String) {
        viewModelScope.launch {
            recentSearchRepository.removeRecentSearchQuery(query)
        }
    }

    fun deactivateSearch() {
        searchState.update { state ->
            state.copy(
                searchQuery = "",
                isSearchActive = false,
                isSearchBarFocused = false,
                isShowingSearchResults = false,
                searchResults = emptyList(),
                isSearchLoading = false,
            )
        }
    }

    private fun HomeSearchState.deactivateIfIdle(): HomeSearchState {
        if (!isSearchBarFocused && searchQuery.isBlank()) {
            return copy(
                isSearchActive = false,
                isShowingSearchResults = false,
                searchResults = emptyList(),
                isSearchLoading = false,
            )
        }
        return this
    }

    private suspend fun fetchSearchResults(
        query: String,
    ): List<AnalyzedImage> {
        return try {
            searchRepository
                .searchSemanticImages(query = query)
                .first()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
