package com.ssafy.modera.core.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(val state: NavigationState) {
    fun navigate(key: NavKey) {
        when (key) {
            state.currentTopLevelKey -> clearSubStack()
            in state.topLevelKeys -> goToTopLevel(key)
            else -> goToKey(key)
        }
    }

    fun popBackStack() {
        if (state.currentSubStack.size > 1) {
            state.currentSubStack.removeLastOrNull()
        }
    }

    fun navigateToHome() {
        val homeKey = state.startKey
        goToTopLevel(homeKey)
        state.subStacks[homeKey]?.apply {
            clear()
            add(homeKey)
        }
    }

    fun isAtTabRoot(): Boolean =
        state.currentSubStack.size == 1

    fun isAtHomeTabRoot(): Boolean =
        state.currentTopLevelKey == state.startKey && isAtTabRoot()

    fun navigateToTopLevelTab(
        topLevelKey: NavKey,
        rootKey: NavKey,
    ) {
        require(topLevelKey in state.topLevelKeys) {
            "$topLevelKey is not a top-level destination"
        }

        goToTopLevel(topLevelKey)
        state.subStacks[topLevelKey]?.apply {
            clear()
            add(rootKey)
        }
    }

    private fun goToKey(key: NavKey) {
        state.currentSubStack.apply {
            remove(key)
            add(key)
        }
    }

    private fun goToTopLevel(key: NavKey) {
        state.topLevelStack.apply {
            clear()
            add(key)
        }
    }

    private fun clearSubStack() {
        state.currentSubStack.run {
            if (size > 1) subList(1, size).clear()
        }
    }
}
