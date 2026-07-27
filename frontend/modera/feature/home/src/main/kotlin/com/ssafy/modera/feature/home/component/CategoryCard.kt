package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.home.R

@Composable
fun CategoryCard(
    title: String,
    imageUrl: String?,
    itemCount: Int,
    tags: List<String>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val tagFormat = stringResource(R.string.home_category_tag_format)

    ClickableSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 10.dp,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(
                        R.string.home_category_image_content_description,
                        title,
                    ),
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop,
                )

                Text(
                    text = itemCount.toString(),
                    style = ModeraTheme.typography.bodySB14,
                    color = ModeraTheme.colors.blue,
                )
            }

            Spacer(modifier = Modifier.size(10.dp))

            Text(
                text = title,
                style = ModeraTheme.typography.titleSB18,
                color = ModeraTheme.colors.typo,
            )

            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = tags.joinToString(separator = "   ") { tag ->
                    tagFormat.format(tag)
                },
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.blue,
                maxLines = 1,
            )
        }
    }
}

@Preview(
    showBackground = true,
)
@Composable
private fun CategoryCardPreview() {
    ModeraTheme {
        Box(
            modifier = Modifier
                .background(Color.Black)
                .padding(16.dp),
        ) {
            CategoryCard(
                title = "쇼핑",
                imageUrl = null,
                itemCount = 12,
                tags = listOf("coor", "무신사", "쿠팡"),
                modifier = Modifier.size(
                    width = 176.dp,
                    height = 140.dp,
                ),
            )
        }
    }
}