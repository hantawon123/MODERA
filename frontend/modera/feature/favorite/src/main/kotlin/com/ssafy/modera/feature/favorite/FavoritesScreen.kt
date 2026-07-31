package com.ssafy.modera.feature.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraScrollToTopButton
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.component.item.ModeraAnalyzedImageItem
import com.ssafy.modera.core.component.rememberShowScrollToTop
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.util.statusBarTopPadding
import kotlinx.coroutines.launch

@Composable
fun FavoritesRoute(
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favorites = remember { FavoritesDummyData.items }

    FavoritesScreen(
        favorites = favorites,
        onItemClick = onItemClick,
        modifier = modifier,
    )
}

@Composable
fun FavoritesScreen(
    favorites: List<AnalyzedImage>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val showScrollToTop = rememberShowScrollToTop(listState)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = ModeraTheme.colors.white)
            .statusBarTopPadding(),
    ) {
        ModeraTopBar(
            leftContent = {
                Text(
                    text = stringResource(R.string.favorites_title),
                    style = ModeraTheme.typography.titleSB20,
                    color = ModeraTheme.colors.gray900,
                    modifier = Modifier.padding(4.dp),
                )
            },
            onBackClick = {},
        )

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.favorites_item_count, favorites.size),
                        style = ModeraTheme.typography.bodyR14,
                        color = ModeraTheme.colors.gray500,
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = ModeraTheme.colors.gray200,
                    )
                }

                items(
                    items = favorites,
                    key = AnalyzedImage::id,
                ) { favorite ->
                    ModeraAnalyzedImageItem(
                        title = favorite.title,
                        description = favorite.summary,
                        tags = favorite.hashtags,
                        imageUrl = favorite.thumbnailUrl,
                        favorite = favorite.favorite,
                        isDocumented = favorite.isDocumented,
                        hasSchedule = favorite.hasSchedule,
                        onClick = { onItemClick(favorite.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

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

private object FavoritesDummyData {
    private val baseItem = AnalyzedImage(
        id = 0L,
        title = "성심당 케이크 리스트",
        summary = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루, 귤 시루, 맛있겠다.",
        hashtags = listOf("기차", "예약", "KTX"),
        thumbnailUrl = "",
        favorite = true,
    )

    val items = List(12) { index ->
        when (index % 4) {
            0 -> baseItem.copy(
                id = (index + 1).toLong(),
                favorite = true,
                isDocumented = true,
                hasSchedule = true,
            )

            1 -> baseItem.copy(
                id = (index + 1).toLong(),
                favorite = false,
                isDocumented = true,
                hasSchedule = true,
            )

            2 -> baseItem.copy(
                id = (index + 1).toLong(),
                favorite = true,
                isDocumented = true,
                hasSchedule = false,
            )

            else -> baseItem.copy(
                id = (index + 1).toLong(),
                favorite = true,
                isDocumented = false,
                hasSchedule = true,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun FavoritesScreenPreview() {
    ModeraTheme {
        FavoritesScreen(
            favorites = FavoritesDummyData.items,
            onItemClick = {},
        )
    }
}
