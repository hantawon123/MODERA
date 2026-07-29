package com.ssafy.modera.core.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.R
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

/**
 * @param onBackClick 뒤로가기 버튼 클릭 시 실행할 콜백
 * @param modifier TopBar 레이아웃 modifier
 * @param centerContent TopBar 중앙 영역 composable
 * @param rightContent TopBar 우측 영역 composable
 */
@Composable
fun ModeraTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit = {},
    rightContent: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left_24),
            contentDescription = "뒤로가기",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(24.dp)
                .clickable(onClick = onBackClick),
            tint = ModeraTheme.colors.gray700,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            centerContent()
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            rightContent()
        }
    }
}

object ModeraTopBarDefaults {
    val IconSize = 24.dp
}

@Preview(showBackground = true, name = "ModeraTopBar - 더보기")
@Composable
private fun ModeraTopBarMoreMenuPreview() {
    ModeraTheme {
        ModeraTopBar(
            onBackClick = {},
            rightContent = {
                Icon(
                    imageVector = ImageVector.vectorResource(ModeraIcons.MoreVertical),
                    contentDescription = "더보기",
                    modifier = Modifier
                        .size(ModeraTopBarDefaults.IconSize)
                        .clickable(onClick = { }),
                    tint = ModeraTheme.colors.gray700,
                )
            },
        )
    }
}

@Preview(showBackground = true, name = "ModeraTopBar - 타이틀 + 텍스트 액션")
@Composable
private fun ModeraTopBarTitleWithTextActionPreview() {
    ModeraTheme {
        ModeraTopBar(
            onBackClick = {},
            centerContent = {
                Text(
                    text = "스크린샷 관리",
                    style = ModeraTheme.typography.bodySB16,
                    color = ModeraTheme.colors.gray900,
                    maxLines = 1,
                )
            },
            rightContent = {
                Text(
                    text = "편집",
                    modifier = Modifier.clickable(onClick = {}),
                    style = ModeraTheme.typography.bodySB14,
                    color = ModeraTheme.colors.blue,
                )
            },
        )
    }
}
