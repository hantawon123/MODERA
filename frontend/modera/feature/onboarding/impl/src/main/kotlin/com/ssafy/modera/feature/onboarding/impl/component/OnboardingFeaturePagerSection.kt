package com.ssafy.modera.feature.onboarding.impl.component

import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.onboarding.impl.OnboardingPhase
import com.ssafy.modera.feature.onboarding.impl.R
import com.ssafy.modera.feature.onboarding.impl.model.OnboardingAnalysisState
import kotlinx.coroutines.launch

@Composable
internal fun BoxWithConstraintsScope.OnboardingFeaturePagerSection(
    phase: OnboardingPhase,
    onAnalysisResultClick: () -> Unit,
    analysisState: OnboardingAnalysisState,
) {
    val screenHeight = maxHeight
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = {
            FEATURE_PAGE_COUNT
        },
    )

    AnimatedVisibility(
        visible = phase == OnboardingPhase.FeaturePager,
        modifier = Modifier.matchParentSize(),
        enter = fadeIn(animationSpec = tween(durationMillis = SECTION_ENTER_DURATION_MILLIS),
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            /*
             * 실제 페이지 영역
             *
             * userScrollEnabled를 막지 않았기 때문에
             * 사용자가 직접 좌우로 넘길 수 있다.
             *
             * 자동 이동은 각 페이지 영상 재생 완료 시점에 수행한다.
             */
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                FeaturePagerPage(
                    page = page,
                    isActive = pagerState.settledPage == page,
                    screenHeight = screenHeight,
                    onVideoFinished = onVideoFinished@{
                        if (
                            phase != OnboardingPhase.FeaturePager ||
                            page != pagerState.settledPage ||
                            pagerState.isScrollInProgress ||
                            page >= LAST_PAGE_INDEX
                        ) {
                            return@onVideoFinished
                        }

                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = page + 1,
                                animationSpec = tween(
                                    durationMillis = PAGE_SCROLL_DURATION_MILLIS,
                                ),
                            )
                        }
                    },
                )
            }

            /*
             * Indicator
             */
            PagerIndicator(
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = screenHeight * INDICATOR_Y_RATIO),
            )

            AnalysisStatusFloatingPill(
                analysisState = analysisState,
                onClick = onAnalysisResultClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = ANALYSIS_STATUS_BOTTOM_PADDING)
                    .zIndex(1f),
            )
        }
    }
}

@Composable
private fun FeaturePagerPage(
    page: Int,
    isActive: Boolean,
    screenHeight: androidx.compose.ui.unit.Dp,
    onVideoFinished: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        FeaturePageTitle(
            page = page,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    y = screenHeight * TITLE_Y_RATIO,
                )
                .fillMaxWidth()
                .padding(
                    horizontal = 32.dp,
                ),
        )

        key(isActive) {
            if (isActive) {
                val videoMaxHeight =
                    screenHeight -
                        (screenHeight * VIDEO_Y_RATIO) -
                        VIDEO_BOTTOM_CLEARANCE

                FeaturePagerVideo(
                    videoRes = when (page) {
                        CALENDAR_PAGE_INDEX -> R.raw.calendar
                        SEARCH_PAGE_INDEX -> R.raw.search
                        else -> R.raw.document
                    },
                    maxHeight = videoMaxHeight,
                    onVideoFinished = onVideoFinished,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(
                            y = screenHeight * VIDEO_Y_RATIO,
                        ),
                )
            }
        }
    }
}

@Composable
private fun FeaturePagerVideo(
    @RawRes videoRes: Int,
    maxHeight: Dp,
    onVideoFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val onVideoFinishedState = rememberUpdatedState(onVideoFinished)
    var aspectRatio by remember {
        mutableFloatStateOf(DEFAULT_VIDEO_ASPECT_RATIO)
    }

    val videoUri = remember(videoRes) {
        Uri.parse("android.resource://${context.packageName}/$videoRes")
    }

    val videoWidth = minOf(
        VIDEO_WIDTH,
        maxHeight * aspectRatio,
    )

    /*
     * VideoView(SurfaceView) 깜빡임을 막기 위해
     * 같은 View 계층의 흰 커버로 가렸다가 첫 프레임 후 fade-out 한다.
     */
    AndroidView(
        factory = { viewContext ->
            val cover = View(viewContext).apply {
                setBackgroundColor(Color.WHITE)
            }

            val videoView = VideoView(viewContext).apply {
                setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                setOnPreparedListener { mediaPlayer ->
                    val width = mediaPlayer.videoWidth
                    val height = mediaPlayer.videoHeight
                    if (width > 0 && height > 0) {
                        aspectRatio = width.toFloat() / height.toFloat()
                    }

                    mediaPlayer.isLooping = false
                    mediaPlayer.setVolume(0f, 0f)
                    mediaPlayer.setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            cover.animate()
                                .alpha(0f)
                                .setDuration(VIDEO_REVEAL_DURATION_MILLIS.toLong())
                                .withEndAction {
                                    cover.visibility = View.GONE
                                }
                                .start()
                            true
                        } else {
                            false
                        }
                    }
                    start()
                }
                setOnCompletionListener {
                    onVideoFinishedState.value()
                }
                setVideoURI(videoUri)
            }

            FrameLayout(viewContext).apply {
                setBackgroundColor(Color.WHITE)
                addView(
                    videoView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                addView(
                    cover,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                tag = videoView
            }
        },
        modifier = modifier
            .width(videoWidth)
            .aspectRatio(aspectRatio),
        onRelease = { container ->
            (container.tag as? VideoView)?.stopPlayback()
        },
    )
}

@Composable
private fun FeaturePageTitle(
    page: Int,
    modifier: Modifier = Modifier,
) {
    when (page) {
        CALENDAR_PAGE_INDEX -> {
            val title = buildAnnotatedString {
                withStyle(
                    style = ModeraTheme.typography.titleB22
                        .toSpanStyle()
                        .copy(
                            color = ModeraTheme.colors.gray900,
                        ),
                ) {
                    append(
                        stringResource(
                            R.string.onboarding_feature_calendar_title_prefix,
                        ),
                    )
                }

                withStyle(
                    style = ModeraTheme.typography.titleB22
                        .toSpanStyle()
                        .copy(
                            color = ModeraTheme.colors.yellow700,
                        ),
                ) {
                    append(" ")
                    append(
                        stringResource(
                            R.string.onboarding_feature_calendar_title_highlight,
                        ),
                    )
                    append(" ")
                }

                withStyle(
                    style = ModeraTheme.typography.titleB22
                        .toSpanStyle()
                        .copy(
                            color = ModeraTheme.colors.gray900,
                        ),
                ) {
                    append(
                        stringResource(
                            R.string.onboarding_feature_calendar_title_suffix,
                        ),
                    )
                }
            }

            Text(
                text = title,
                modifier = modifier,
                textAlign = TextAlign.Center,
            )
        }

        SEARCH_PAGE_INDEX -> {
            Text(
                text = stringResource(
                    R.string.onboarding_feature_document_search,
                ),
                modifier = modifier,
                style = ModeraTheme.typography.titleB22,
                color = ModeraTheme.colors.gray900,
                textAlign = TextAlign.Center,
            )
        }

        DOCUMENT_PAGE_INDEX -> {
            Text(
                text = stringResource(
                    R.string.onboarding_feature_document_title,
                ),
                modifier = modifier,
                style = ModeraTheme.typography.titleB22,
                color = ModeraTheme.colors.gray900,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PagerIndicator(
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(FEATURE_PAGE_COUNT) { index ->
            val indicatorColor by animateColorAsState(
                targetValue = if (currentPage == index) {
                    ModeraTheme.colors.yellow700
                } else {
                    ModeraTheme.colors.gray300
                },
                animationSpec = tween(
                    durationMillis = INDICATOR_ANIMATION_DURATION_MILLIS,
                ),
                label = "pagerIndicatorColor",
            )

            Box(
                modifier = Modifier
                    .padding(
                        horizontal = INDICATOR_HORIZONTAL_SPACING,
                    )
                    .size(INDICATOR_SIZE)
                    .background(
                        color = indicatorColor,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

private const val FEATURE_PAGE_COUNT = 3

private const val CALENDAR_PAGE_INDEX = 0
private const val SEARCH_PAGE_INDEX = 1
private const val DOCUMENT_PAGE_INDEX = 2

private const val LAST_PAGE_INDEX =
    FEATURE_PAGE_COUNT - 1

private const val INDICATOR_Y_RATIO = 0.105f
private const val TITLE_Y_RATIO = 0.145f
private const val VIDEO_Y_RATIO = 0.25f
private val ANALYSIS_STATUS_BOTTOM_PADDING = 40.dp

/*
 * 하단 AnalysisStatus 버튼(최대 56dp) + padding + 여유 공간.
 */
private val VIDEO_BOTTOM_CLEARANCE = 120.dp

private val VIDEO_WIDTH = 300.dp
private const val DEFAULT_VIDEO_ASPECT_RATIO = 1078f / 1924f

private val INDICATOR_SIZE = 10.dp
private val INDICATOR_HORIZONTAL_SPACING = 5.dp

private const val SECTION_ENTER_DURATION_MILLIS = 400
private const val INDICATOR_ANIMATION_DURATION_MILLIS = 220
private const val PAGE_SCROLL_DURATION_MILLIS = 600
private const val VIDEO_REVEAL_DURATION_MILLIS = 180
