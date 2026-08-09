package com.ssafy.modera.feature.analyzedimage.detail.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.modera.core.designsystem.component.IconButton
import com.ssafy.modera.core.designsystem.component.ModeraIconButtonDefaults
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
internal fun ImageSection(
    imageUrl: String,
    onImageExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(
                width = 1.dp,
                color = ModeraTheme.colors.gray500,
            ),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "원본 이미지",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            IconButton(
                painter = painterResource(ModeraIcons.ImageExpand),
                contentDescription = "이미지 전체 화면으로 보기",
                onClick = onImageExpandClick,
                size = 28.dp,
                colors = ModeraIconButtonDefaults.iconButtonColors(
                    contentColor = Color.Unspecified
                )
            )
        }
    }
}

@Preview(
    name = "ImageSection",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun ImageSectionPreview() {
    ModeraTheme {
        ImageSection(
            imageUrl = "https://picsum.photos/600/400",
            onImageExpandClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}