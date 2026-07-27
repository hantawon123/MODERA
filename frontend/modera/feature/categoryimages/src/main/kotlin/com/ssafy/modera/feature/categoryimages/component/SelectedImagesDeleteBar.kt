package com.ssafy.modera.feature.categoryimages.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Button
import com.ssafy.modera.core.designsystem.component.ModeraButtonDefaults
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.categoryimages.R

@Composable
internal fun SelectedImagesDeleteBar(
    selectedCount: Int,
    enabled: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ModeraTheme.colors.white)
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
    ) {
        Button(
            onClick = onDeleteClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ModeraButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF4D4F),
                contentColor = ModeraTheme.colors.white,
                disabledContainerColor = Color(0xFFE5E7EB),
                disabledContentColor = Color(0xFF9CA3AF),
            ),
            contentPadding = PaddingValues(
                vertical = 14.dp,
            ),
        ) {
            Text(
                text = if (selectedCount > 0) {
                    stringResource(
                        R.string.category_image_list_delete_count,
                        selectedCount,
                    )
                } else {
                    stringResource(
                        R.string.category_image_list_delete,
                    )
                },
                style = ModeraTheme.typography.bodySB14,
            )
        }
    }
}

@Preview(
    name = "Delete Selection Bar - Enabled",
    showBackground = true,
)
@Composable
private fun SelectedImagesDeleteBarEnabledPreview() {
    ModeraTheme {
        SelectedImagesDeleteBar(
            selectedCount = 3,
            enabled = true,
            onDeleteClick = {},
        )
    }
}

@Preview(
    name = "Delete Selection Bar - Disabled",
    showBackground = true,
)
@Composable
private fun SelectedImagesDeleteBarDisabledPreview() {
    ModeraTheme {
        SelectedImagesDeleteBar(
            selectedCount = 0,
            enabled = false,
            onDeleteClick = {},
        )
    }
}