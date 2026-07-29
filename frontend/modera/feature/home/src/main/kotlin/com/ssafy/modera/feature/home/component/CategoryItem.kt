package com.ssafy.modera.feature.home.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraCategoryIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.home.R

/**
 * 홈 카테고리 타일 — 제목을 받아 대표 일러스트를 매핑해 보여준다.
 *
 * @param title 카테고리 제목 (예: "음식", "쇼핑")
 * @param onClick 카드 클릭 콜백
 */
@Composable
fun CategoryItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val illustrationRes = categoryIllustrationRes(title)

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
                .padding(start = 16.dp, top = 16.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.TopStart),
                style = ModeraTheme.typography.titleSB18,
                color = ModeraTheme.colors.gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Image(
                painter = painterResource(illustrationRes),
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

/**
 * 카테고리 제목 → 대표 일러스트 drawable 매핑.
 * 알 수 없는 제목은 [ModeraCategoryIcons.Etc]로 처리한다.
 */
@DrawableRes
internal fun categoryIllustrationRes(title: String): Int = when (title.trim()) {
    "쇼핑" -> ModeraCategoryIcons.Shopping
    "음식" -> ModeraCategoryIcons.Food
    "여행" -> ModeraCategoryIcons.Travel
    "예약" -> ModeraCategoryIcons.Reservation
    "할인" -> ModeraCategoryIcons.Discount
    "금융" -> ModeraCategoryIcons.Finance
    "미용" -> ModeraCategoryIcons.Beauty
    "학습" -> ModeraCategoryIcons.Learning
    "취업" -> ModeraCategoryIcons.Job
    "IT" -> ModeraCategoryIcons.It
    "뉴스" -> ModeraCategoryIcons.News
    "부동산" -> ModeraCategoryIcons.RealEstate
    "건강" -> ModeraCategoryIcons.Health
    "엔터" -> ModeraCategoryIcons.Entertainment
    "자동차" -> ModeraCategoryIcons.Automotive
    "반려동물" -> ModeraCategoryIcons.Pet
    else -> ModeraCategoryIcons.Etc
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
        Box(modifier = Modifier.padding(16.dp)) {
            CategoryItem(
                title = "음식",
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
        Box(modifier = Modifier.padding(16.dp)) {
            CategoryItem(
                title = "쇼핑",
                onClick = {},
                modifier = Modifier.size(160.dp),
            )
        }
    }
}
