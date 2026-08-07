package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.home.R

/**
 * 홈 카테고리 타일.
 *
 * @param title 카테고리 제목
 * @param imageUrl 카테고리 대표 이미지 URL
 * @param isNew 새로 생성된 카테고리 여부
 * @param onClick 카드 클릭 콜백
 */
@Composable
fun CategoryItem(
    title: String,
    imageUrl: String?,
    isNew: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ClickableSurface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = CategoryItemDefaults.Shape,
        color = ModeraTheme.colors.gray50,
        shadowElevation = CategoryItemDefaults.ShadowElevation,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                ),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(
                        weight = 1f,
                        fill = false,
                    ),
                    style = ModeraTheme.typography.bodySB14,
                    color = ModeraTheme.colors.gray900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (isNew) {
                    NewBadge()
                }
            }

            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(
                    R.string.home_category_image_content_description,
                    title,
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxSize(CategoryItemDefaults.IllustrationSizeFraction),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun NewBadge(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(ModeraTheme.colors.yellow900Bg)
            .padding(
                horizontal = 6.dp,
                vertical = 2.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.category_new_badge),
            style = ModeraTheme.typography.captionSB10,
            color = ModeraTheme.colors.white,
        )
    }
}

private object CategoryItemDefaults {
    val Shape = RoundedCornerShape(20.dp)
    val ShadowElevation = 6.dp

    const val IllustrationSizeFraction = 0.80f
}

@Preview(showBackground = true)
@Composable
private fun CategoryItemFoodPreview() {
    ModeraTheme {
        Box(
            modifier = Modifier.padding(16.dp),
        ) {
            CategoryItem(
                title = "음식",
                imageUrl = "",
                isNew = true,
                onClick = {},
                modifier = Modifier.size(160.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryItemShoppingPreview() {
    ModeraTheme {
        Box(
            modifier = Modifier.padding(16.dp),
        ) {
            CategoryItem(
                title = "쇼핑",
                imageUrl = "",
                isNew = false,
                onClick = {},
                modifier = Modifier.size(160.dp),
            )
        }
    }
}