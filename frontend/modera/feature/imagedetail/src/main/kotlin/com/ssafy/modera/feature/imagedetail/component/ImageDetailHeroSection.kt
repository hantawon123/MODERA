package com.ssafy.modera.feature.imagedetail.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.imagedetail.R

@Composable
internal fun ImageDetailHeroSection(
    imageUrl: String,
    title: String,
    showOverlay: Boolean,
    categoryName: String,
    uploadedAt: String,
    hashtags: List<String>,
    isFavorite: Boolean,
    onImageClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val heroHeight = LocalConfiguration.current.screenHeightDp.dp * 0.58f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight),
    ) {
        ClickableSurface(
            onClick = onImageClick,
            color = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(
                    R.string.image_detail_hero_content_description,
                    title,
                ),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f),
                            ),
                        ),
                    ),
            )
        }

        AnimatedVisibility(
            visible = showOverlay,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ImageDetailHeroMetadata(
                categoryName = categoryName,
                title = title,
                uploadedAt = uploadedAt,
                hashtags = hashtags,
                isFavorite = isFavorite,
                onCategoryClick = onCategoryClick,
                onFavoriteClick = onFavoriteClick,
            )
        }
    }
}

@Composable
private fun ImageDetailHeroMetadata(
    categoryName: String,
    title: String,
    uploadedAt: String,
    hashtags: List<String>,
    isFavorite: Boolean,
    onCategoryClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                ClickableSurface(
                    onClick = onCategoryClick,
                    color = ModeraTheme.colors.yellow500.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = categoryName,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp,
                        ),
                        style = ModeraTheme.typography.captionSB12,
                        color = ModeraTheme.colors.white,
                    )
                }

                Text(
                    text = title,
                    style = ModeraTheme.typography.bodySB14,
                    color = ModeraTheme.colors.white,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            if (isFavorite) {
                ClickableSurface(
                    onClick = onFavoriteClick,
                    color = Color.Transparent,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_star_filled),
                        contentDescription = stringResource(R.string.image_detail_favorite),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(24.dp),
                        tint = Color.Unspecified,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.image_detail_uploaded_at, uploadedAt),
            style = ModeraTheme.typography.captionR12,
            color = ModeraTheme.colors.white.copy(alpha = 0.85f),
        )

        if (hashtags.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                hashtags.forEach { hashtag ->
                    Text(
                        text = stringResource(
                            R.string.image_detail_hashtag,
                            hashtag.removePrefix("#"),
                        ),
                        style = ModeraTheme.typography.captionM12,
                        color = ModeraTheme.colors.white.copy(alpha = 0.9f),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 480)
@Composable
private fun ImageDetailHeroSectionPreview() {
    ModeraTheme {
        ImageDetailHeroSection(
            imageUrl = "https://picsum.photos/seed/hackathon/800/1200",
            title = "ASCII HACKATHON",
            showOverlay = true,
            categoryName = "개발",
            uploadedAt = "2026년 7월 17일 금요일 오후 7:23",
            hashtags = listOf("해커톤", "SW"),
            isFavorite = true,
            onImageClick = {},
            onCategoryClick = {},
            onFavoriteClick = {},
        )
    }
}