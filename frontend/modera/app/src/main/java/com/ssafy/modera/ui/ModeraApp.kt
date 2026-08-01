package com.ssafy.modera.ui

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.ssafy.modera.MainViewModel
import com.ssafy.modera.core.designsystem.component.ModeraBottomNavigation
import com.ssafy.modera.core.designsystem.component.ModeraBottomNavigationAction
import com.ssafy.modera.core.designsystem.component.ModeraBottomNavigationItem
import com.ssafy.modera.core.designsystem.component.ModeraNavigationSuiteScaffold
import com.ssafy.modera.core.designsystem.component.Scaffold
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.core.navigation.toEntries
import com.ssafy.modera.feature.analyzedimagedetail.navigation.analyzedImageDetailEntry
import com.ssafy.modera.feature.analyzedimagedetail.navigation.navigateToImageDetail
import com.ssafy.modera.feature.calendar.navigation.calendarEntry
import com.ssafy.modera.feature.calendar.navigation.navigateToCalendar
import com.ssafy.modera.feature.category.navigation.CategoryNavKey
import com.ssafy.modera.feature.category.navigation.categoryEntry
import com.ssafy.modera.feature.category.navigation.navigateToCategorySearch
import com.ssafy.modera.feature.category.navigation.navigateToCategoryTab
import com.ssafy.modera.feature.categoryimages.navigation.categoryImagesEntry
import com.ssafy.modera.feature.documentcreate.navigation.documentCreateEntry
import com.ssafy.modera.feature.documentcreate.navigation.navigateToDocumentCreate
import com.ssafy.modera.feature.favorite.navigation.favoritesEntry
import com.ssafy.modera.feature.home.HomeAnalysisState
import com.ssafy.modera.feature.home.LocalHomeAnalysisState
import com.ssafy.modera.feature.home.navigation.HomeNavKey
import com.ssafy.modera.feature.home.navigation.homeEntry
import com.ssafy.modera.feature.imageviewer.navigation.imageViewerEntry
import com.ssafy.modera.feature.imageviewer.navigation.navigateToImageViewer
import com.ssafy.modera.feature.relatedimages.navigation.relatedImagesEntry
import com.ssafy.modera.media.rememberGalleryPickerLauncher
import com.ssafy.modera.navigation.BOTTOM_NAV_ITEMS
import com.ssafy.modera.navigation.DocumentsNavKey
import com.ssafy.modera.navigation.TOP_LEVEL_NAV_ITEMS

@Composable
fun ModeraApp(
    appState: ModeraAppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    viewModel: MainViewModel = hiltViewModel(),
) {
//    val shouldShowGradientBackground = appState.navigationState.currentTopLevelKey == ForYouNavKey
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val localSnackbarHostState = compositionLocalOf<SnackbarHostState> {
        error("SnackbarHostState state should be initialized at runtime")
    }

    CompositionLocalProvider(localSnackbarHostState provides snackbarHostState) {
        ModeraApp(
            appState = appState,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            windowAdaptiveInfo = windowAdaptiveInfo,
            modifier = modifier,
        )
    }
}

@Composable
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
internal fun ModeraApp(
    appState: ModeraAppState,
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = remember { Navigator(appState.navigationState) }
    val handleBack = rememberModeraBackHandler(navigator)
    val homeAnalysisState = remember(viewModel) {
        HomeAnalysisState(onDismissRequest = viewModel::dismissAnalysisBanner)
    }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }

    LaunchedEffect(uiState.showAnalysisBanner, uiState.analysisImageCount) {
        homeAnalysisState.sync(
            showBanner = uiState.showAnalysisBanner,
            imageCount = uiState.analysisImageCount,
        )
    }

    val launchGalleryPicker = rememberGalleryPickerLauncher(
        onImagesPicked = { images ->
            if (images.isEmpty()) return@rememberGalleryPickerLauncher
            viewModel.onImagesPicked(images)
            navigator.navigate(HomeNavKey)
        },
    )

    val isTopLevelDestination =
        appState.navigationState.currentSubStack.size == 1

    CompositionLocalProvider(
        LocalHomeAnalysisState provides homeAnalysisState,
    ) {
        ModeraNavigationSuiteScaffold(
            layoutType = NavigationSuiteType.None,
            navigationSuiteItems = {},
            windowAdaptiveInfo = windowAdaptiveInfo,
        ) {
            Scaffold(
                modifier = modifier,
                containerColor = Color.Transparent,
                contentColor = Color.Gray, // TODO : 추후 컬러 변경
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (isTopLevelDestination) {
                        ModeraBottomNavigation {
                            BOTTOM_NAV_ITEMS.forEach { (navKey, navItem) ->
                                if (navItem.isCenterAction) {
                                    ModeraBottomNavigationAction(
                                        onClick = launchGalleryPicker,
                                        iconRes = navItem.icon,
                                        contentDescription = stringResource(navItem.titleTextId),
                                    )
                                } else {
                                    val labelRes = navItem.iconTextId
                                        ?: error("Bottom nav item requires iconTextId: $navKey")
                                    ModeraBottomNavigationItem(
                                        selected = navKey == appState.navigationState.currentTopLevelKey,
                                        onClick = {
                                            if (navKey == CategoryNavKey()) {
                                                navigator.navigateToCategoryTab(
                                                    selectedCategoryId = null,
                                                )
                                            } else {
                                                navigator.navigate(navKey)
                                            }
                                        },
                                        iconRes = navItem.icon,
                                        label = stringResource(labelRes),
                                    )
                                }
                            }
                        }
                    }
                },
                snackbarHost = {
                    SnackbarHost(
                        snackbarHostState,
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.exclude(
                                WindowInsets.ime,
                            ),
                        ),
                    )
                },
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal,
                            ),
                        ),
                ) {
                    // Only show the top app bar on top level destinations.
                    var shouldShowTopAppBar = false

                    if (appState.navigationState.currentSubStack.size == 1) {
                        shouldShowTopAppBar = true

                        val destination =
                            TOP_LEVEL_NAV_ITEMS[appState.navigationState.currentTopLevelKey]
                                ?: error("Top level nav item not found for ${appState.navigationState.currentTopLevelKey}")
                    }

                    Box(
                        modifier = Modifier.consumeWindowInsets(
                            if (shouldShowTopAppBar) {
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                            } else {
                                WindowInsets(0, 0, 0, 0)
                            },
                        ),
                    ) {
                        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

                        SharedTransitionLayout {
                            ModeraBackHandler(onBack = handleBack)

                            val entryProvider = entryProvider {
                                homeEntry(
                                    onCategoryClick = navigator::navigateToCategoryTab,
                                    onCalendarClick = navigator::navigateToCalendar,
                                    onSearchResultClick = navigator::navigateToImageDetail,
                                )
                                categoryEntry(
                                    onBackClick = handleBack,
                                    onSearchIconClick = navigator::navigateToCategorySearch,
                                    onItemClick = { /* TODO: 자료 상세 연결 */ },
                                )
                                favoritesEntry(
                                    onItemClick = { /* TODO: 자료 상세 연결 */ },
                                )
                                calendarEntry(
                                    onBackClick = handleBack,
                                )
                                documentsEntry()
                                categoryImagesEntry(
                                    navigator = navigator,
                                    onImageClick = navigator::navigateToImageDetail,
                                    onBackClick = handleBack,
                                )
                                analyzedImageDetailEntry(
                                    navigator = navigator,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    onImageClick = navigator::navigateToImageViewer,
                                    onBackClick = handleBack,
                                    onCreateDocumentClick = navigator::navigateToDocumentCreate
                                )
                                imageViewerEntry(
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    onBackClick = handleBack,
                                )
                                relatedImagesEntry(
                                    navigator = navigator,
                                    onBackClick = handleBack,
                                )
                                documentCreateEntry(
                                    navigator = navigator,
                                    onBackClick = handleBack,
                                )
                            }

                            NavDisplay(
                                entries = appState.navigationState.toEntries(entryProvider),
//                        sceneStrategy = listDetailStrategy,
                                onBack = handleBack,
                            )
                        }
                    }
                }
            }
        }
    }
}

fun EntryProviderScope<NavKey>.documentsEntry() {
    entry<DocumentsNavKey> {}
}
