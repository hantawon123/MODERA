package com.ssafy.modera.ui

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.ssafy.modera.MainViewModel
import com.ssafy.modera.R
import com.ssafy.modera.core.designsystem.component.ModeraBottomNavigation
import com.ssafy.modera.core.designsystem.component.ModeraBottomNavigationAction
import com.ssafy.modera.core.designsystem.component.ModeraBottomNavigationItem
import com.ssafy.modera.core.designsystem.component.ModeraNavigationSuiteScaffold
import com.ssafy.modera.core.designsystem.component.Scaffold
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.core.navigation.toEntries
import com.ssafy.modera.core.ui.LoadingScreen
import com.ssafy.modera.core.ui.snackbar.ModeraSnackbarEffect
import com.ssafy.modera.core.ui.snackbar.ModeraSnackbarMessage
import com.ssafy.modera.core.ui.snackbar.ModeraSnackbarProvider
import com.ssafy.modera.core.ui.snackbar.rememberModeraSnackbarHostState
import com.ssafy.modera.fcm.NotificationNavigationTarget
import com.ssafy.modera.feature.analyzedimagedetail.navigation.AnalyzedImageDetailNavKey
import com.ssafy.modera.feature.analyzedimagedetail.navigation.analyzedImageDetailEntry
import com.ssafy.modera.feature.analyzedimagedetail.navigation.navigateToImageDetail
import com.ssafy.modera.feature.calendar.navigation.calendarEntry
import com.ssafy.modera.feature.calendar.navigation.navigateToCalendar
import com.ssafy.modera.feature.category.navigation.categoryEntry
import com.ssafy.modera.feature.category.navigation.navigateToCategoryTab
import com.ssafy.modera.feature.document.documentcreate.navigation.documentCreateEntry
import com.ssafy.modera.feature.document.documentcreate.navigation.navigateToDocumentCreate
import com.ssafy.modera.feature.document.documentcreate.navigation.navigateToDocumentRecreate
import com.ssafy.modera.feature.document.documentdetail.navigation.DocumentDetailNavKey
import com.ssafy.modera.feature.document.documentdetail.navigation.documentDetailEntry
import com.ssafy.modera.feature.document.documentedit.navigation.documentEditEntry
import com.ssafy.modera.feature.document.documentedit.navigation.navigateToDocumentEdit
import com.ssafy.modera.feature.document.documents.navigation.DocumentNavKey
import com.ssafy.modera.feature.document.documents.navigation.documentEntry
import com.ssafy.modera.feature.favorite.navigation.favoritesEntry
import com.ssafy.modera.feature.home.navigation.HomeNavKey
import com.ssafy.modera.feature.home.navigation.homeEntry
import com.ssafy.modera.feature.home.navigation.navigateToHomeTab
import com.ssafy.modera.feature.imageviewer.navigation.imageViewerEntry
import com.ssafy.modera.feature.imageviewer.navigation.navigateToImageViewer
import com.ssafy.modera.feature.login.LoginRoute
import com.ssafy.modera.feature.relatedimages.navigation.relatedImagesEntry
import com.ssafy.modera.feature.settings.navigation.navigateToSettings
import com.ssafy.modera.feature.settings.navigation.settingsEntry
import com.ssafy.modera.media.rememberGalleryPickerLauncher
import com.ssafy.modera.navigation.BOTTOM_NAV_ITEMS
import com.ssafy.modera.navigation.TOP_LEVEL_NAV_ITEMS
import com.ssafy.modera.navigation.moderaPopTransition
import com.ssafy.modera.navigation.moderaPredictivePopTransition
import com.ssafy.modera.navigation.moderaPushTransition
import com.ssafy.modera.session.AppSessionUiState
import com.ssafy.modera.session.AppSessionViewModel

@Composable
fun ModeraApp(
    appState: ModeraAppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    notificationNavigationTarget: NotificationNavigationTarget? = null,
    onNotificationNavigationConsumed: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
    sessionViewModel: AppSessionViewModel = hiltViewModel(),
) {
    val snackbarHostState = rememberModeraSnackbarHostState()
    val logoutMessage = stringResource(R.string.logout_success)
    val sessionUiState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val navigator = remember(appState.navigationState) {
        Navigator(appState.navigationState)
    }

    ModeraSnackbarProvider(
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        ModeraSnackbarEffect(sessionViewModel)

        when (sessionUiState) {
            AppSessionUiState.Loading -> {
                LoadingScreen(modifier = Modifier.fillMaxSize())
            }

            AppSessionUiState.Unauthenticated -> {
                LoginRoute(modifier = Modifier.fillMaxSize())
            }

            AppSessionUiState.Authenticated -> {
                ModeraApp(
                    appState = appState,
                    viewModel = viewModel,
                    windowAdaptiveInfo = windowAdaptiveInfo,
                    onLogoutClick = {
                        navigator.resetToStart()
                        sessionViewModel.logout(
                            ModeraSnackbarMessage(
                                message = logoutMessage,
                                iconRes = ModeraIcons.Check,
                            ),
                        )
                    },
                    notificationNavigationTarget = notificationNavigationTarget,
                    onNotificationNavigationConsumed = onNotificationNavigationConsumed,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
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
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    notificationNavigationTarget: NotificationNavigationTarget? = null,
    onNotificationNavigationConsumed: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    ModeraSnackbarEffect(viewModel)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = remember {
        Navigator(appState.navigationState)
    }

    LaunchedEffect(notificationNavigationTarget) {
        when (
            val target = notificationNavigationTarget
        ) {
            is NotificationNavigationTarget.ImageDetail -> {
                navigator.navigateToNotificationImageDetail(
                    imageId = target.imageId,
                )
            }

            is NotificationNavigationTarget.DocumentDetail -> {
                navigator.navigateToDocumentDetail(
                    documentId = target.documentId,
                )
            }

            null -> {
                return@LaunchedEffect
            }
        }

        onNotificationNavigationConsumed()
    }


    val handleBack = rememberModeraBackHandler(navigator)

    val launchGalleryPicker = rememberGalleryPickerLauncher(
        onImagesPicked = { images ->
            if (images.isEmpty()) {
                return@rememberGalleryPickerLauncher
            }

            viewModel.onImagesPicked(images)
            navigator.navigate(HomeNavKey)
        },
    )

    val isTopLevelDestination =
        appState.navigationState.currentSubStack.size == 1

    CompositionLocalProvider {
        ModeraNavigationSuiteScaffold(
            layoutType = NavigationSuiteType.None,
            navigationSuiteItems = {},
            windowAdaptiveInfo = windowAdaptiveInfo,
        ) {
            Scaffold(
                modifier = modifier,
                containerColor = Color.Transparent,
                contentColor = Color.Gray,
                contentWindowInsets = WindowInsets(
                    left = 0,
                    top = 0,
                    right = 0,
                    bottom = 0,
                ),
                bottomBar = {
                    if (isTopLevelDestination) {
                        ModeraBottomNavigation {
                            BOTTOM_NAV_ITEMS.forEach { (navKey, navItem) ->
                                if (navItem.isCenterAction) {
                                    ModeraBottomNavigationAction(
                                        onClick = launchGalleryPicker,
                                        iconRes = navItem.icon,
                                        contentDescription = stringResource(
                                            navItem.titleTextId,
                                        ),
                                    )
                                } else {
                                    val labelRes = navItem.iconTextId
                                        ?: error(
                                            "Bottom nav item requires iconTextId: $navKey",
                                        )

                                    ModeraBottomNavigationItem(
                                        selected =
                                            navKey ==
                                                    appState.navigationState
                                                        .currentTopLevelKey,
                                        onClick = {
                                            when (navKey) {
                                                HomeNavKey -> {
                                                    navigator.navigateToHomeTab(
                                                        appState.homeTabController,
                                                    )
                                                }

                                                else -> {
                                                    navigator.navigate(navKey)
                                                }
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
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal,
                            ),
                        ),
                ) {
                    var shouldShowTopAppBar = false

                    if (
                        appState.navigationState.currentSubStack.size == 1
                    ) {
                        shouldShowTopAppBar = true

                        TOP_LEVEL_NAV_ITEMS[
                            appState.navigationState.currentTopLevelKey
                        ] ?: error(
                            "Top level nav item not found for " +
                                    "${appState.navigationState.currentTopLevelKey}",
                        )
                    }

                    Box(
                        modifier = Modifier.consumeWindowInsets(
                            if (shouldShowTopAppBar) {
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Top,
                                )
                            } else {
                                WindowInsets(
                                    left = 0,
                                    top = 0,
                                    right = 0,
                                    bottom = 0,
                                )
                            },
                        ),
                    ) {
                        val listDetailStrategy =
                            rememberListDetailSceneStrategy<NavKey>()

                        SharedTransitionLayout {
                            ModeraBackHandler(
                                onBack = handleBack,
                            )

                            val entryProvider = entryProvider {
                                homeEntry(
                                    onCategoryClick =
                                        navigator::navigateToCategoryTab,
                                    onCalendarClick =
                                        navigator::navigateToCalendar,
                                    onSettingsClick =
                                        navigator::navigateToSettings,
                                    onSearchResultClick =
                                        navigator::navigateToImageDetail,
                                )

                                settingsEntry(
                                    onBackClick = handleBack,
                                    onLogoutClick = onLogoutClick,
                                )

                                categoryEntry(
                                    onItemClick =
                                        navigator::navigateToImageDetail,
                                )

                                favoritesEntry(
                                    onItemClick = {
                                        // TODO: 자료 상세 연결
                                    },
                                )

                                calendarEntry(
                                    onBackClick = handleBack,
                                )
                                analyzedImageDetailEntry(
                                    navigator = navigator,
                                    sharedTransitionScope =
                                        this@SharedTransitionLayout,
                                    onImageClick =
                                        navigator::navigateToImageViewer,
                                    onBackClick = handleBack,
                                    onCreateDocumentClick =
                                        navigator::navigateToDocumentCreate,
                                )

                                imageViewerEntry(
                                    sharedTransitionScope =
                                        this@SharedTransitionLayout,
                                    onBackClick = handleBack,
                                )

                                relatedImagesEntry(
                                    navigator = navigator,
                                    onBackClick = handleBack,
                                )

                                documentEntry(
                                    onDocumentClick = navigator::navigateToDocumentDetail,
                                )

                                documentDetailEntry(
                                    onBackClick = handleBack,
                                    onManageImagesClick = navigator::navigateToDocumentEdit,
                                )

                                documentEditEntry(
                                    onBackClick = handleBack,
                                    onEditClick = { documentId, analyzedImages ->
                                        navigator.navigateToDocumentRecreate(
                                            documentId = documentId,
                                            analyzedImages = analyzedImages,
                                        )
                                    },
                                    onImageClick = navigator::navigateToImageDetail,
                                )

                                documentCreateEntry(
                                    navigator = navigator,
                                    onBackClick = handleBack,
                                    onDocumentCreated = navigator::navigateToDocumentDetail,
                                )
                            }

                            NavDisplay(
                                entries = appState.navigationState.toEntries(
                                    entryProvider,
                                ),
                                transitionSpec = {
                                    moderaPushTransition()
                                },
                                popTransitionSpec = {
                                    moderaPopTransition()
                                },
                                predictivePopTransitionSpec = {
                                    moderaPredictivePopTransition()
                                },
                                onBack = handleBack,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Navigator.navigateToDocumentDetail(
    documentId: Long,
) {
    navigateToTopLevelTab(
        topLevelKey = DocumentNavKey,
        rootKey = DocumentNavKey,
    )

    navigate(
        DocumentDetailNavKey(
            documentId = documentId,
        ),
    )
}

private fun Navigator.navigateToNotificationImageDetail(
    imageId: Long,
) {
    navigateToTopLevelTab(
        topLevelKey = HomeNavKey,
        rootKey = HomeNavKey,
    )

    navigate(
        AnalyzedImageDetailNavKey(
            imageId = imageId,
        ),
    )
}
