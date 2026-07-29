package com.ssafy.modera.feature.analyzedimagedetail.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
internal fun RelatedImagesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = ModeraTheme.colors.yellow800,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = 18.dp,
                vertical = 16.dp,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(ModeraIcons.FileSearch),
            contentDescription = null,
            tint = ModeraTheme.colors.yellow800,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "연관 자료 보러가기",
            color = ModeraTheme.colors.yellow800,
            style = ModeraTheme.typography.bodySB16
        )
    }
}

@Preview(
    name = "RelatedImagesButton",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun RelatedImagesButtonPreview() {
    ModeraTheme {
        RelatedImagesButton(
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}