package com.ssafy.modera.feature.analyzedimagedetail.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ssafy.modera.core.designsystem.component.IconButton
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

// 공통 UI 구성 후 폐기
@Composable
fun DetailAppBar(
    menuExpanded: Boolean,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onDocumentClick: () -> Unit,
    onReanalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
    ) {
        IconButton(
            painter = painterResource(ModeraIcons.ArrowLeft),
            contentDescription = "뒤로 가기",
            onClick = onBackClick,
            size = 24.dp,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        Box(
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            IconButton(
                painter = painterResource(ModeraIcons.MoreVertical),
                contentDescription = "더보기",
                size = 24.dp,
                onClick = onMoreClick,
            )

            if (menuExpanded) {
                DetailMenu(
                    onDismissRequest = onDismissMenu,
                    onDocumentClick = onDocumentClick,
                    onReanalyzeClick = onReanalyzeClick,
                    onDeleteClick = onDeleteClick,
                )
            }
        }
    }
}

@Composable
private fun DetailMenu(
    onDismissRequest: () -> Unit,
    onDocumentClick: () -> Unit,
    onReanalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top = 48.dp,
                    end = 8.dp,
                )
                .width(184.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(12.dp),
                    clip = false,
                )
                .clip(RoundedCornerShape(12.dp))
                .background(ModeraTheme.colors.white),
        ) {
            DetailMenuItem(
                iconRes = ModeraIcons.FileDocument,
                text = "문서화하기",
                onClick = onDocumentClick,
            )

            DetailMenuItem(
                iconRes = ModeraIcons.Refresh,
                text = "재분석하기",
                onClick = onReanalyzeClick,
            )

            DetailMenuItem(
                iconRes = ModeraIcons.Trash,
                text = "삭제하기",
                contentColor = ModeraTheme.colors.red,
                onClick = onDeleteClick,
            )
        }
    }
}

@Composable
private fun DetailMenuItem(
    iconRes: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = ModeraTheme.colors.gray900,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 18.dp,
                vertical = 15.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )

        Text(
            text = text,
            style = ModeraTheme.typography.bodyR16.copy(
                color = contentColor,
            ),
        )
    }
}

@Preview(
    name = "DetailTopBar - Menu Closed",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun DetailAppBarClosedPreview() {
    ModeraTheme {
        DetailAppBar(
            menuExpanded = false,
            onBackClick = {},
            onMoreClick = {},
            onDismissMenu = {},
            onDocumentClick = {},
            onReanalyzeClick = {},
            onDeleteClick = {},
        )
    }
}

@Preview(
    name = "DetailTopBar - Menu Open",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun DetailAppBarOpenedPreview() {
    ModeraTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            DetailAppBar(
                menuExpanded = true,
                onBackClick = {},
                onMoreClick = {},
                onDismissMenu = {},
                onDocumentClick = {},
                onReanalyzeClick = {},
                onDeleteClick = {},
            )
        }
    }
}

@Preview(
    name = "DetailTopBar - Interactive",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun DetailAppBarInteractivePreview() {
    ModeraTheme {
        var menuExpanded by remember {
            mutableStateOf(false)
        }

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            DetailAppBar(
                menuExpanded = menuExpanded,
                onBackClick = {},
                onMoreClick = {
                    menuExpanded = true
                },
                onDismissMenu = {
                    menuExpanded = false
                },
                onDocumentClick = {
                    menuExpanded = false
                },
                onReanalyzeClick = {
                    menuExpanded = false
                },
                onDeleteClick = {
                    menuExpanded = false
                },
            )
        }
    }
}