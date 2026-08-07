package com.ssafy.modera.feature.analyzedimage.related.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.analyzedimage.related.R

@Composable
internal fun RelatedHeader(
    sourceTitle: String,
    relatedImageCount: Int,
    headerSuffix: String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val sourceTitleStyle = ModeraTheme.typography.bodySB16
    val suffixStyle = ModeraTheme.typography.bodyR16
    val badgeStyle = ModeraTheme.typography.captionSB12

    val sourceTitleFormat = stringResource(
        R.string.related_source_title_format,
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val badgeText = relatedImageCount.toString()

        val badgeTextWidth = textMeasurer.measure(
            text = badgeText,
            style = badgeStyle,
            maxLines = 1,
        ).size.width

        val badgeHorizontalPadding = with(density) {
            16.dp.roundToPx()
        }

        val spacerWidth = with(density) {
            8.dp.roundToPx()
        }

        val headerMaxWidth = (
                constraints.maxWidth -
                        badgeTextWidth -
                        badgeHorizontalPadding -
                        spacerWidth
                ).coerceAtLeast(0)

        val displaySourceTitle = fitSourceTitle(
            sourceTitle = sourceTitle,
            sourceTitleFormat = sourceTitleFormat,
            headerSuffix = headerSuffix,
            maxWidth = headerMaxWidth,
            sourceTitleStyle = sourceTitleStyle,
            suffixStyle = suffixStyle,
            textMeasurer = textMeasurer,
        )

        val formattedSourceTitle = sourceTitleFormat.format(
            displaySourceTitle,
        )

        val headerText = buildAnnotatedString {
            withStyle(
                style = sourceTitleStyle
                    .toSpanStyle()
                    .copy(
                        color = ModeraTheme.colors.gray900,
                    ),
            ) {
                append(formattedSourceTitle)
            }

            withStyle(
                style = suffixStyle
                    .toSpanStyle()
                    .copy(
                        color = ModeraTheme.colors.gray700,
                    ),
            ) {
                append(headerSuffix)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = headerText,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )

            Spacer(
                modifier = Modifier.width(8.dp),
            )

            Text(
                text = badgeText,
                style = badgeStyle,
                color = ModeraTheme.colors.yellow800,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ModeraTheme.colors.yellow700Bg)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 2.dp,
                    ),
            )
        }
    }
}

private fun fitSourceTitle(
    sourceTitle: String,
    sourceTitleFormat: String,
    headerSuffix: String,
    maxWidth: Int,
    sourceTitleStyle: TextStyle,
    suffixStyle: TextStyle,
    textMeasurer: TextMeasurer,
): String {
    fun fits(
        title: String,
    ): Boolean {
        val formattedSourceTitle = sourceTitleFormat.format(title)

        val text = buildAnnotatedString {
            withStyle(
                style = sourceTitleStyle.toSpanStyle(),
            ) {
                append(formattedSourceTitle)
            }

            withStyle(
                style = suffixStyle.toSpanStyle(),
            ) {
                append(headerSuffix)
            }
        }

        return textMeasurer.measure(
            text = text,
            maxLines = 1,
        ).size.width <= maxWidth
    }

    if (fits(sourceTitle)) {
        return sourceTitle
    }

    if (!fits(ELLIPSIS)) {
        return ""
    }

    var low = 0
    var high = sourceTitle.length

    while (low < high) {
        val mid = (low + high + 1) / 2
        val candidate = sourceTitle.take(mid) + ELLIPSIS

        if (fits(candidate)) {
            low = mid
        } else {
            high = mid - 1
        }
    }

    return sourceTitle.take(low) + ELLIPSIS
}

private const val ELLIPSIS = "…"

@Preview(
    name = "Related Images Header",
    showBackground = true,
)
@Composable
private fun RelatedHeaderPreview() {
    ModeraTheme {
        RelatedHeader(
            sourceTitle = "ASCII 해커톤 정리 ASCII 해커톤 정리 ASCII 해커톤 정리",
            relatedImageCount = 3,
            headerSuffix = "와 관련된 자료",
            modifier = Modifier.padding(20.dp),
        )
    }
}