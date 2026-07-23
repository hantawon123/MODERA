package com.ssafy.modera.feature.imagedetail.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Button
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.ModeraButtonDefaults
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.imagedetail.R

@Composable
internal fun ImageDetailActionBar(
    onReanalyzeClick: () -> Unit,
    onRelatedMaterialsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(ModeraTheme.colors.white)
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClickableSurface(
            onClick = onReanalyzeClick,
            color = ModeraTheme.colors.gray,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(52.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.image_detail_reanalyze),
                    modifier = Modifier.size(24.dp),
                    tint = ModeraTheme.colors.typo,
                )
            }
        }

        Button(
            onClick = onRelatedMaterialsClick,
            modifier = Modifier.weight(1f),
            colors = ModeraButtonDefaults.buttonColors(
                containerColor = ModeraTheme.colors.blue.copy(alpha = 0.12f),
                contentColor = ModeraTheme.colors.blue,
            ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = stringResource(R.string.image_detail_related_materials),
                style = ModeraTheme.typography.body2SemiBold,
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp),
                tint = ModeraTheme.colors.blue,
            )
        }
    }
}

@Composable
internal fun ImageDetailActionBarAnimated(
    visible: Boolean,
    onReanalyzeClick: () -> Unit,
    onRelatedMaterialsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
    ) {
        ImageDetailActionBar(
            onReanalyzeClick = onReanalyzeClick,
            onRelatedMaterialsClick = onRelatedMaterialsClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImageDetailActionBarPreview() {
    ModeraTheme {
        Column {
            ImageDetailActionBar(
                onReanalyzeClick = {},
                onRelatedMaterialsClick = {},
            )
        }
    }
}
