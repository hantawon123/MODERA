package com.ssafy.modera.feature.document.documentdetail.component

import android.text.method.LinkMovementMethod
import android.util.TypedValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.ssafy.modera.core.designsystem.R
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import android.widget.TextView as AndroidTextView

@Composable
fun DocumentMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textColor = ModeraTheme.colors.gray900.toArgb()
    val linkColor = ModeraTheme.colors.yellow700.toArgb()

    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .build()
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { androidContext ->
            AndroidTextView(androidContext).apply {
                includeFontPadding = false

                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    14f,
                )

                setLineSpacing(
                    0f,
                    1.4f,
                )

                setPadding(
                    0,
                    0,
                    0,
                    0,
                )

                typeface = ResourcesCompat.getFont(
                    androidContext,
                    R.font.pretendard_variable,
                )

                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
                setTextIsSelectable(true)
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)

            markwon.setMarkdown(
                textView,
                markdown,
            )
        },
    )
}

@Preview(name = "DocumentMarkdownText", showBackground = true)
@Composable
private fun DocumentMarkdownTextPreview() {
    ModeraTheme {
        DocumentMarkdownText(
            markdown = """
                ## AI 도시·지역혁신 아이디어 공모전
                
                국토교통부가 주관하는 공모전으로, AI 기술을 활용한 도시 및
                지역혁신 아이디어를 모집합니다.
                
                - **공모분야:** 건설혁신형, 주거개선형, 혁신행정형
                - **참가대상:** 관련 분야 종사자 및 대학생
                - **접수기간:** 2026년 8월 28일 18시까지
                - **문의처:** 도시·지역혁신 산업박람회 운영사무국
                
                > 출처: #52
                
                ---
                
                ## 정보처리기사 국가기술자격 정보
                
                큐넷을 통해 제공되는 정보처리기사 국가기술자격의 기본 정보입니다.
                
                - **자격증명:** 정보처리기사
                - **관련부처:** 과학기술정보통신부
                - **시행기관:** 한국산업인력공단
                
                ## 출처
                
                | 이미지 | 제목 | 카테고리 |
                | --- | --- | --- |
                | #52 | AI 도시·지역혁신 아이디어 공모전 | 일정 |
                | #61 | 정보처리기사 국가자격 종목 상세정보 | 학습 |
            """.trimIndent(),
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        )
    }
}