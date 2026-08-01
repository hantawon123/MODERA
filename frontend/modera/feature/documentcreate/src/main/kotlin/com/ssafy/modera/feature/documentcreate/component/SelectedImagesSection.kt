package com.ssafy.modera.feature.documentcreate.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.modera.core.designsystem.component.IconButton
import com.ssafy.modera.core.designsystem.component.ModeraIconButtonDefaults
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.feature.documentcreate.R
import kotlinx.coroutines.flow.first

@Composable
internal fun SelectedImagesSection(
    images: List<AnalyzedImage>,
    onRemoveClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    var previousItemCount by remember {
        mutableIntStateOf(images.size)
    }

    LaunchedEffect(images.size) {
        if (images.size > previousItemCount) {
            snapshotFlow {
                listState.layoutInfo.totalItemsCount
            }.first { totalItemCount ->
                totalItemCount == images.size
            }

            val layoutInfo = listState.layoutInfo

            val itemWidthPx = with(density) {
                72.dp.toPx()
            }

            val itemSpacingPx =
                layoutInfo.mainAxisItemSpacing.toFloat()

            val totalContentWidthPx =
                layoutInfo.beforeContentPadding +
                        itemWidthPx * images.size +
                        itemSpacingPx *
                        (images.size - 1).coerceAtLeast(0) +
                        layoutInfo.afterContentPadding

            val targetScrollPosition =
                (
                        totalContentWidthPx -
                                layoutInfo.viewportSize.width
                        ).coerceAtLeast(0f)

            val currentScrollPosition =
                listState.firstVisibleItemIndex *
                        (itemWidthPx + itemSpacingPx) +
                        listState.firstVisibleItemScrollOffset

            val scrollDistance =
                targetScrollPosition - currentScrollPosition

            if (scrollDistance > 0f) {
                listState.animateScrollBy(
                    value = scrollDistance,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        }

        previousItemCount = images.size
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
    ) {
        Text(
            text = stringResource(
                R.string.document_create_selected_images,
            ),
            style = ModeraTheme.typography.captionR12,
            color = ModeraTheme.colors.gray700,
            modifier = Modifier.padding(start = 24.dp)
        )

        Spacer(Modifier.height(8.dp))

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            itemsIndexed(
                items = images,
                key = { _, image -> image.id },
            ) { index, image ->
                SelectedImageItem(
                    image = image,
                    isPrimary = index == 0,
                    canRemove = images.size > 1,
                    onRemoveClick = {
                        onRemoveClick(image.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectedImageItem(
    image: AnalyzedImage,
    isPrimary: Boolean,
    canRemove: Boolean,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val removeContentDescription = stringResource(
        R.string.document_create_remove_selected_image,
        image.title,
    )

    Box(
        modifier = modifier.size(72.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(68.dp)
                .then(
                    if (isPrimary) {
                        Modifier
                            .border(
                                width = 1.dp,
                                color = ModeraTheme.colors.yellow700Bg,
                                shape = RoundedCornerShape(6.dp),
                            )
                    } else {
                        Modifier
                    },
                )
                .padding(4.dp),
        ) {
            AsyncImage(
                model = image.thumbnailUrl,
                contentDescription = stringResource(
                    R.string.document_create_selected_image_description,
                    image.title,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        if (canRemove) {
            IconButton(
                imageVector = ImageVector.vectorResource(ModeraIcons.Close),
                onClick = onRemoveClick,
                size = 24.dp,
                colors = ModeraIconButtonDefaults.iconButtonColors(
                    containerColor = ModeraTheme.colors.black.copy(alpha = 0.42f),
                    contentColor = ModeraTheme.colors.white
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = removeContentDescription
                    }
            )
        }
    }
}

@Preview(
    name = "선택된 자료 - 1개",
    showBackground = true,
    widthDp = 360,
)
@Composable
private fun SelectedImagesSectionSinglePreview() {
    ModeraTheme {
        SelectedImagesSection(
            images = previewAnalyzedImages.take(1),
            onRemoveClick = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Preview(
    name = "선택된 자료 - 여러 개",
    showBackground = true,
    widthDp = 360,
)
@Composable
private fun SelectedImagesSectionMultiplePreview() {
    ModeraTheme {
        SelectedImagesSection(
            images = previewAnalyzedImages,
            onRemoveClick = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

private val previewAnalyzedImages = List(5) { index ->
    AnalyzedImage(
        id = index.toLong(),
        title = "분석 이미지 ${index + 1}",
        summary = "문서화를 위해 선택된 분석 이미지입니다.",
        thumbnailUrl = "https://picsum.photos/seed/document-$index/300/300",
        hashtags = listOf("여행", "일정", "예약"),
        favorite = false,
    )
}