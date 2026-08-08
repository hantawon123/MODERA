package com.ssafy.modera.feature.document.documentdetail.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraDocumentInfoRow
import com.ssafy.modera.core.component.ModeraTooltip
import com.ssafy.modera.core.designsystem.component.Button
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.IconButton
import com.ssafy.modera.core.designsystem.component.ModeraButtonDefaults
import com.ssafy.modera.core.designsystem.component.ModeraIconButtonDefaults
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.DocumentDetail
import com.ssafy.modera.feature.documentdetail.R

@Composable
internal fun DocumentDetailHeader(
    document: DocumentDetail,
    tooltipExpanded: Boolean,
    onManageImagesClick: () -> Unit,
    onTooltipClick: () -> Unit,
    onTooltipDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = document.name,
            style = ModeraTheme.typography.titleB20,
            color = ModeraTheme.colors.gray900,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        ModeraDocumentInfoRow(
            imageCount = document.imageCount,
            updatedAt = document.updatedAt,
            contentColor = ModeraTheme.colors.gray500,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DocumentManageImagesButton(
                onClick = onManageImagesClick,
            )

            DocumentManageImagesTooltipButton(
                expanded = tooltipExpanded,
                onClick = onTooltipClick,
                onDismissRequest = onTooltipDismiss,
            )
        }
    }
}

@Composable
private fun DocumentManageImagesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ModeraButtonDefaults.buttonColors(
            containerColor = ModeraTheme.colors.white,
            contentColor = ModeraTheme.colors.gray500,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = ModeraTheme.colors.gray200,
        ),
        contentPadding = PaddingValues(
            horizontal = 10.dp,
            vertical = 7.dp,
        ),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(
                ModeraIcons.Images,
            ),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = ModeraTheme.colors.gray400,
        )

        Spacer(
            modifier = Modifier.width(5.dp),
        )

        Text(
            text = stringResource(
                R.string.document_manage_screenshots,
            ),
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray500,
            maxLines = 1,
        )
    }
}

@Composable
private fun DocumentManageImagesTooltipButton(
    expanded: Boolean,
    onClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            imageVector = ImageVector.vectorResource(
                ModeraIcons.InformationCircle,
            ),
            contentDescription = stringResource(
                R.string.document_manage_screenshots_information,
            ),
            size = 16.dp,
            colors = ModeraIconButtonDefaults.iconButtonColors(
                contentColor = ModeraTheme.colors.gray400,
            ),
            onClick = onClick,
        )

        ModeraTooltip(
            expanded = expanded,
            text = stringResource(
                R.string.document_manage_screenshots_tooltip,
            ),
            onDismissRequest = onDismissRequest,
        )
    }
}

private val previewDocumentDetail = DocumentDetail(
    id = 1L,
    name = "AI 도시·지역혁신 공모전 및 정보처리기사 자격 정보",
    summary = "AI 기술을 접목한 도시 및 지역혁신 아이디어 공모전 안내와 " +
            "정보처리기사 국가기술자격의 기본 정보를 정리한 문서입니다.",
    content = "",
    imageCount = 8,
    deletedImageCount = 0,
    imageIds = listOf(52L, 61L, 62L, 63L, 64L, 65L, 66L, 67L),
    regenerating = false,
    updatedAt = 1_785_593_950_561L,
)

@Preview(name = "DocumentDetailHeader", showBackground = true)
@Composable
private fun DocumentDetailHeaderPreview() {
    ModeraTheme {
        DocumentDetailHeader(
            document = previewDocumentDetail,
            tooltipExpanded = false,
            onManageImagesClick = {},
            onTooltipClick = {},
            onTooltipDismiss = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(ModeraTheme.colors.white)
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
        )
    }
}