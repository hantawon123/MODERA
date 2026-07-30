package com.ssafy.modera.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.model.category.CategorySortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
) : ViewModel() {
    private val searchState = MutableStateFlow(SearchState())

    val uiState: StateFlow<HomeUiState> =
        combine(
            categoryRepository
                .getCategories(CategorySortType.NAME_ASC)
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

    private fun SearchState.deactivateIfIdle(): SearchState {
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
    ): List<SearchMaterialResult> {
        delay(HomeSearchDefaults.SearchLoadingDelayMillis)
        // TODO: 검색 API 연동
        return HomeSearchDummyData.searchResults
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

    private data class SearchState(
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val isSearchBarFocused: Boolean = false,
        val recentSearchTerms: List<String> = HomeSearchDummyData.recentSearchTerms,
        val searchResults: List<SearchMaterialResult> = emptyList(),
        val isShowingSearchResults: Boolean = false,
        val isSearchLoading: Boolean = false,
    )

    private companion object {
        private object HomeSearchDefaults {
            const val MaxRecentSearchTermCount = 10
            const val SearchLoadingDelayMillis = 2_000L
        }
    }
}
