package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.item.ModeraAnalyzedImageItem
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.feature.home.R

@Composable
internal fun SearchResultSection(
    searchResults: List<AnalyzedImage>,
    isLoading: Boolean,
    onSearchResultClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> {
            SearchAnalyzingScreen(
                modifier = modifier.fillMaxSize(),
            )
        }

        searchResults.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_search_result_empty),
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.gray500,
                    textAlign = TextAlign.Center,
                )
            }
        }

        else -> {
            Column(
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_search_result_comment),
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.gray500,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                ) {
                    items(
                        items = searchResults,
                        key = { it.id },
                    ) { result ->
                        ModeraAnalyzedImageItem(
                            title = result.title,
                            description = result.summary,
                            tags = result.hashtags,
                            favorite = result.favorite,
                            imageUrl = result.thumbnailUrl.takeIf(String::isNotBlank),
                            onClick = { onSearchResultClick(result.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 480, name = "Search Results")
@Composable
private fun SearchResultSectionPreview() {
    ModeraTheme {
        SearchResultSection(
            searchResults = listOf(
                AnalyzedImage(
                    id = 1,
                    title = "성심당 케이크 리스트",
                    summary = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
                    thumbnailUrl = "",
                    hashtags = listOf("기차", "예약", "KTX"),
                ),
                AnalyzedImage(
                    id = 2,
                    title = "주말 브런치 레시피",
                    summary = "에그 베네딕트와 팬케이크 레시피 모음.",
                    thumbnailUrl = "",
                    hashtags = listOf("음식", "레시피"),
                ),
            ),
            isLoading = false,
            onSearchResultClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 480, name = "Search Analyzing")
@Composable
private fun SearchResultSectionLoadingPreview() {
    ModeraTheme {
        SearchResultSection(
            searchResults = emptyList(),
            isLoading = true,
            onSearchResultClick = {},
        )
    }
}
