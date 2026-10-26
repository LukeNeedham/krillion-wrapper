package com.lukeneedham.krillionwrapper.game

import android.webkit.CookieManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lukeneedham.krillionwrapper.BuildConfig
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Krillion web game, full screen. In a debug build the settings gear opens a sheet with the debug tools, which can
 * open the shared library's debug pages ([onOpenDebug]).
 */
@Composable
fun GamePage(onOpenDebug: () -> Unit) {
    val viewModel = koinViewModel<GameViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(viewModel) {
        viewModel.attach(context)
        onDispose { viewModel.detach() }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.webView.onResume()
        onPauseOrDispose {
            viewModel.webView.onPause()
            CookieManager.getInstance().flush() // persist cookies in case the process is killed
        }
    }
    // Back closes what is over the game first, then goes back in the game's own history; only then does it leave the app.
    BackHandler(enabled = uiState.settingsOpen || uiState.inspectorOpen || uiState.canGoBack) {
        when {
            uiState.settingsOpen -> viewModel.closeSettings()
            uiState.inspectorOpen -> viewModel.closeInspector()
            else -> viewModel.goBack()
        }
    }

    GamePageContent(
        uiState = uiState,
        webView = viewModel.webView,
        showSettings = BuildConfig.DEBUG,
        onRetry = viewModel::retry,
        onOpenSettings = viewModel::openSettings,
        onCloseSettings = viewModel::closeSettings,
        onOpenDebug = onOpenDebug,
        onToggleInspector = viewModel::toggleInspector,
        onShowLayoutReport = {
            viewModel.closeSettings()
            LayoutReport.show(context, viewModel.webView)
        },
    )
}
