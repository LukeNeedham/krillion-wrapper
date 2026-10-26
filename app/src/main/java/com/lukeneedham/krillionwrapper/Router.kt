package com.lukeneedham.krillionwrapper

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.lukeneedham.common.ui.CrashesPage
import com.lukeneedham.common.ui.DebugPage
import com.lukeneedham.common.ui.LogsPage
import com.lukeneedham.krillionwrapper.game.GamePage

/**
 * Calls the page of [route], with the navigation as lambdas. The only place that calls a page. The debug pages are
 * always routable, but only a debug build has anything that opens them (the game page's settings sheet).
 */
@Composable
fun Router(backStack: NavBackStack<NavKey>, route: Route) {
    // The common actions are defined once here, never repeated in each branch
    fun navigateTo(target: Route) { backStack.add(target) }
    fun onBack() { backStack.removeLastOrNull() }

    when (route) { // exhaustive: no `else`
        Route.Game -> GamePage(onOpenDebug = { navigateTo(Route.Debug) })
        Route.Debug -> DebugPage(
            onBack = ::onBack,
            onOpenLogs = { navigateTo(Route.Logs) },
            onOpenCrashes = { navigateTo(Route.Crashes) },
        )
        Route.Logs -> LogsPage(onBack = ::onBack)
        Route.Crashes -> CrashesPage(onBack = ::onBack)
    }
}
