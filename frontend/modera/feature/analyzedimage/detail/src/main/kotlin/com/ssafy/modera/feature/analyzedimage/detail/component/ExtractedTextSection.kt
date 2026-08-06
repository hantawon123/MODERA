package com.ssafy.modera.feature.analyzedimage.detail.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.analyzedimage.detail.R

@Composable
internal fun ExtractedTextSection(
    title: String,
    content: String,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "extractedTextArrowRotation",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = ModeraTheme.typography.bodySB16,
                color = ModeraTheme.colors.gray900,
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        role = Role.Button,
                        onClick = onExpandClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(ModeraIcons.ArrowDown),
                    contentDescription = stringResource(
                        if (expanded) {
                            R.string.analyzed_image_detail_collapse_extracted_text
                        } else {
                            R.string.analyzed_image_detail_expand_extracted_text
                        },
                    ),
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(arrowRotation),
                    tint = ModeraTheme.colors.gray700,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = content,
                    style = ModeraTheme.typography.bodyR16,
                    color = ModeraTheme.colors.gray700,
                )
            }
        }
    }
}

@Preview(
    name = "ExtractedTextSection - Collapsed",
    showBackground = true,
)
@Composable
private fun ExtractedTextSectionCollapsedPreview() {
    ModeraTheme {
        ExtractedTextSection(
            title = "추출된 텍스트",
            content = "SSAFY는 진짜 프로그램을 잘 짜는 인력을 양성합니다.",
            expanded = false,
            onExpandClick = {},
        )
    }
}

@Preview(
    name = "ExtractedTextSection - Expanded",
    showBackground = true,
)
@Composable
private fun ExtractedTextSectionExpandedPreview() {
    ModeraTheme {
        ExtractedTextSection(
            title = "추출된 텍스트",
            content = """
                SSAFY
                진짜 프로그램을 잘 짜는 인력 양성
                본인이 직접 프로그램을 작성하는 능력
                정확성과 성능을 생각할 수 있는 역량
            """.trimIndent(),
            expanded = true,
            onExpandClick = {},
        )
    }
}