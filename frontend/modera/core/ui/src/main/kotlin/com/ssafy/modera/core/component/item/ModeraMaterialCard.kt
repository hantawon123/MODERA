package com.ssafy.modera.core.component.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.modera.core.component.ModeraHashtags
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
fun ModeraMaterialCard(
    title: String,
    description: String,
    tags: List<String>,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ItemShape)
            .border(
                width = 1.dp,
                color = ModeraTheme.colors.gray500,
                shape = ItemShape,
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp, horizontal = 4.dp),
        ) {
            Text(
                text = title,
                style = ModeraTheme.typography.bodySB16,
                color = ModeraTheme.colors.gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = description,
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (tags.isNotEmpty()) {

                ModeraHashtags(
                    tags = tags,
                    color = ModeraTheme.colors.gray400,
                    style = ModeraTheme.typography.captionR12,
                )
            }
        }

        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ModeraTheme.colors.gray200),
            contentScale = ContentScale.Crop,
        )
    }
}

private val ItemShape = RoundedCornerShape(6.dp)

@Preview(name = "ModeraMaterialCard", showBackground = true)
@Composable
private fun ModeraMaterialCardPreview() {
    ModeraTheme {
        ModeraMaterialCard(
            title = "성심당 케이크 리스트",
            description = "올해 성심당 케이크 메뉴 리스트로, 샤인머스켓 시루",
            tags = listOf("기차", "예약", "KTX"),
            imageUrl = "",
            onClick = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}