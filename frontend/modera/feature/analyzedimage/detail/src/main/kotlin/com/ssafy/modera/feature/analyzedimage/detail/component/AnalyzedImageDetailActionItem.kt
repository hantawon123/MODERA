package com.ssafy.modera.feature.analyzedimage.detail.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
internal fun AnalyzedImageDetailActionItem(
    @DrawableRes iconRes: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(48.dp)
            .height(56.dp)
            .background(
                color = ModeraTheme.colors.gray100,
                shape = RoundedCornerShape(4.dp),
            )
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                ModeraTheme.colors.gray300,
            ),
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = text,
            style = ModeraTheme.typography.captionR12,
            color = ModeraTheme.colors.gray500,
        )
    }
}

@Preview(
    name = "Analyzed Image Detail Action Item",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun AnalyzedImageDetailActionItemPreview() {
    ModeraTheme {
        AnalyzedImageDetailActionItem(
            iconRes = ModeraIcons.FileDocument,
            text = "문서",
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}