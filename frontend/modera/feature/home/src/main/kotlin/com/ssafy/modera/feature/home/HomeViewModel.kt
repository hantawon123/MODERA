package com.ssafy.modera.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.data.repository.search.SearchRepository
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.category.CategorySortType
import com.ssafy.modera.feature.home.state.HomeSearchState
import com.ssafy.modera.feature.home.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val searchState = MutableStateFlow(HomeSearchState())

    val uiState: StateFlow<HomeUiState> =
        combine(
            categoryRepository
                .getCategories(CategorySortType.UPDATED_DESC)
                .asResult(),
            searchState,
        ) { result, search ->
            when (result) {
                is Result.Success -> {
                    HomeUiState.Success(
                        categories = result.data,
                        searchQuery = search.searchQuery,
                        isSearchActive = search.isSearchActive,
                        recentSearchTerms = search.recentSearchTerms,
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
                initialValue = HomeUiState.Loading,
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
                recentSearchTerms = addRecentSearchTerm(
                    terms = state.recentSearchTerms,
                    term = trimmedQuery,
                ),
                isShowingSearchResults = true,
                isSearchLoading = true,
                searchResults = emptyList(),
            )
        }

        viewModelScope.launch {
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

    fun selectRecentSearch(term: String) {
        searchState.update { state ->
            state.copy(
                searchQuery = term,
                isSearchActive = true,
            )
        }
        submitSearch()
    }

    fun removeRecentSearchTerm(term: String) {
        searchState.update { state ->
            state.copy(
                recentSearchTerms = state.recentSearchTerms.filterNot { it == term },
            )
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

    private fun addRecentSearchTerm(
        terms: List<String>,
        term: String,
    ): List<String> {
        return buildList {
            add(term)
            addAll(terms.filterNot { it == term })
        }.take(HomeSearchDefaults.MaxRecentSearchTermCount)
    }

    private companion object {
        private object HomeSearchDefaults {
            const val MaxRecentSearchTermCount = 10
        }
    }
}
