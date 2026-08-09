package com.ssafy.modera.feature.document.documentdetail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.component.ModeraConfirmDialog
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.DocumentDetail
import com.ssafy.modera.core.ui.ErrorScreen
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.feature.document.documentdetail.component.DocumentDetailHeader
import com.ssafy.modera.feature.document.documentdetail.component.DocumentDetailSkeleton
import com.ssafy.modera.feature.document.documentdetail.component.DocumentDetailTopBar
import com.ssafy.modera.feature.document.documentdetail.component.DocumentMarkdownText
import com.ssafy.modera.feature.documentdetail.R

// Todo: topbar scroll 중복 코드 제거
private val TopBarTitleScrollThreshold = 96.dp

@Composable
internal fun DocumentDetailScreen(
    viewModel: DocumentDetailViewModel,
    onBackClick: () -> Unit,
    onManageImagesClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DocumentDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onManageImagesClick = onManageImagesClick,
        onReanalyzeClick = viewModel::regenerateDocument,
        onDeleteClick = {
            viewModel.deleteDocument(onDeleted = onBackClick)
        },
        modifier = modifier,
    )
}

@Composable
private fun DocumentDetailScreen(
    uiState: DocumentDetailUiState,
    onBackClick: () -> Unit,
    onManageImagesClick: (Long) -> Unit,
    onReanalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember {
        mutableStateOf(false)
    }

    var deleteDialogVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(uiState) {
        if (uiState !is DocumentDetailUiState.Success) {
            menuExpanded = false
            deleteDialogVisible = false
        }
    }

    val scrollState = rememberScrollState()
    val title = if (
        uiState is DocumentDetailUiState.Success
    ) {
        uiState.document.name
    } else {
        ""
    }

    val titleScrollThresholdPx = with(LocalDensity.current) {
        TopBarTitleScrollThreshold.roundToPx()
    }

    val topBarTitle by remember(
        scrollState,
        title,
        titleScrollThresholdPx,
    ) {
        derivedStateOf {
            if (scrollState.value >= titleScrollThresholdPx) {
                title
            } else {
                ""
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModeraTheme.colors.white),
    ) {
        DocumentDetailTopBar(
            title = topBarTitle,
            menuVisible = uiState is DocumentDetailUiState.Success,
            menuExpanded = menuExpanded,
            onBackClick = onBackClick,
            onMoreClick = {
                menuExpanded = !menuExpanded
            },
            onDismissMenu = {
                menuExpanded = false
            },
            onReanalyzeClick = {
                menuExpanded = false
                onReanalyzeClick()
            },
            onDeleteClick = {
                menuExpanded = false
                deleteDialogVisible = true
            },
        )

        when (uiState) {
            DocumentDetailUiState.Loading -> {
                LoadingScreen(
                    modifier = Modifier.weight(1f),
                )
            }

            DocumentDetailUiState.Reanalyzing -> {
                DocumentDetailSkeleton(
                    scrollState = scrollState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                )
            }

            is DocumentDetailUiState.Success -> {
                DocumentDetailContent(
                    document = uiState.document,
                    scrollState = scrollState,
                    onManageImagesClick = {
                        onManageImagesClick(uiState.document.id)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                )
            }

            is DocumentDetailUiState.Error -> {
                ErrorScreen(
                    message = stringResource(
                        R.string.document_detail_load_error,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (
            deleteDialogVisible &&
            uiState is DocumentDetailUiState.Success
        ) {
            ModeraConfirmDialog(
                icon = painterResource(ModeraIcons.Trash),
                targetTitle = uiState.document.name,
                title = stringResource(
                    R.string.document_detail_delete_dialog_title,
                ),
                description = stringResource(
                    R.string.document_detail_delete_dialog_description,
                ),
                confirmText = stringResource(
                    R.string.document_detail_delete_dialog_confirm,
                ),
                dismissText = stringResource(
                    R.string.document_detail_delete_dialog_cancel,
                ),
                confirmButtonColor = ModeraTheme.colors.red,
                onConfirm = {
                    deleteDialogVisible = false
                    onDeleteClick()
                },
                onDismiss = {
                    deleteDialogVisible = false
                },
            )
        }
    }
}

@Composable
private fun DocumentDetailContent(
    document: DocumentDetail,
    scrollState: ScrollState,
    onManageImagesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tooltipExpanded by remember(document.id) {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 40.dp),
    ) {
        DocumentDetailHeader(
            document = document,
            tooltipExpanded = tooltipExpanded,
            onManageImagesClick = onManageImagesClick,
            onTooltipClick = {
                tooltipExpanded = !tooltipExpanded
            },
            onTooltipDismiss = {
                tooltipExpanded = false
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(
                R.string.document_summary_title,
            ),
            style = ModeraTheme.typography.bodySB16,
            color = ModeraTheme.colors.gray900,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = document.summary,
            style = ModeraTheme.typography.bodyR16,
            color = ModeraTheme.colors.gray700,
        )

        Spacer(
            modifier = Modifier.size(28.dp),
        )

        DocumentMarkdownText(
            markdown = document.content,
        )
    }
}

@Preview(name = "DocumentDetailScreen", showBackground = true)
@Composable
private fun DocumentDetailScreenPreview(
    @PreviewParameter(
        DocumentDetailScreenPreviewParameterProvider::class,
    )
    previewData: DocumentDetailScreenPreviewData,
) {
    ModeraTheme {
        DocumentDetailScreen(
            uiState = previewData.uiState,
            onBackClick = {},
            onManageImagesClick = {},
            onReanalyzeClick = {},
            onDeleteClick = {},
        )
    }
}