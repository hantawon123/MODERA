package com.ssafy.modera.core.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

/**
 * 확인용 공통 다이얼로그.
 *
 * @param icon 상단 원형 배경 안에 표시할 아이콘
 * @param targetTitle 대상 아이템 제목 (따옴표로 감싸 표시)
 * @param title 다이얼로그 제목
 * @param description 다이얼로그 본문 설명
 * @param confirmText 오른쪽(확인) 버튼 텍스트
 * @param dismissText 왼쪽(취소) 버튼 텍스트
 * @param confirmButtonColor 오른쪽 버튼 배경색 (아이콘 tint에도 사용)
 * @param onConfirm 오른쪽 버튼 클릭
 * @param onDismiss 왼쪽 버튼 클릭
 * @param onDismissRequest 바깥 영역/백 버튼으로 닫을 때
 */
@Composable
fun ModeraConfirmDialog(
    icon: Painter,
    title: String,
    targetTitle: String = "",
    description: String = "",
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButtonColor: Color = ModeraTheme.colors.red,
    onDismissRequest: () -> Unit = onDismiss,
    iconContentDescription: String? = null,
    properties: DialogProperties = DialogProperties(),
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(ConfirmDialogDefaults.Shape)
                .background(ModeraTheme.colors.white)
                .padding(ConfirmDialogDefaults.ContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = ModeraTheme.colors.gray100,
                        shape = CircleShape,
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = iconContentDescription,
                    modifier = Modifier.size(ConfirmDialogDefaults.IconSize),
                    tint = confirmButtonColor,
                )
            }

            Spacer(modifier = Modifier.height(ConfirmDialogDefaults.IconTextSpacing))

            if (targetTitle.isNotBlank()) {
                Text(
                    text = "\"$targetTitle\"",
                    style = ModeraTheme.typography.bodySB16,
                    color = ModeraTheme.colors.gray900,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(ConfirmDialogDefaults.TitleSpacing))

            Text(
                text = title,
                style = ModeraTheme.typography.titleSB18,
                color = ModeraTheme.colors.gray900,
                textAlign = TextAlign.Center,
            )

            if (description.isNotBlank()) {
                Spacer(modifier = Modifier.height(ConfirmDialogDefaults.DescriptionSpacing))

                Text(
                    text = description,
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.gray500,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(ConfirmDialogDefaults.ButtonTopSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ConfirmDialogDefaults.ButtonGap),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ConfirmDialogDefaults.ButtonShape)
                        .background(
                            color = ModeraTheme.colors.gray200,
                            shape = ConfirmDialogDefaults.ButtonShape,
                        )
                        .clickable(onClick = onDismiss)
                        .padding(ConfirmDialogDefaults.ButtonContentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dismissText,
                        style = ModeraTheme.typography.bodySB16,
                        color = ModeraTheme.colors.gray700,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ConfirmDialogDefaults.ButtonShape)
                        .background(
                            color = confirmButtonColor,
                            shape = ConfirmDialogDefaults.ButtonShape,
                        )
                        .clickable(onClick = onConfirm)
                        .padding(ConfirmDialogDefaults.ButtonContentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = confirmText,
                        style = ModeraTheme.typography.bodySB16,
                        color = ModeraTheme.colors.white,
                    )
                }
            }
        }
    }
}

object ConfirmDialogDefaults {
    val Shape = RoundedCornerShape(20.dp)
    val ButtonShape = RoundedCornerShape(10.dp)
    val ContentPadding = 24.dp
    val IconSize = 24.dp
    val IconTextSpacing = 20.dp
    val TitleSpacing = 4.dp
    val DescriptionSpacing = 8.dp
    val ButtonTopSpacing = 22.dp
    val ButtonGap = 8.dp
    val ButtonContentPadding = PaddingValues(vertical = 12.dp)
}

@Preview(showBackground = true, backgroundColor = 0xFF8D8C86)
@Composable
private fun ModeraConfirmDialogPreview() {
    ModeraTheme {
        Box(
            modifier = Modifier
                .width(360.dp)
                .padding(24.dp),
        ) {
            ModeraConfirmDialog(
                icon = painterResource(ModeraIcons.Trash),
                targetTitle = "SSAFY 중간발표",
                title = "해당 일정을 삭제하시겠어요?",
                description = "삭제된 일정은 복구할 수 없습니다.",
                confirmText = "삭제",
                dismissText = "취소",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
