package com.lukeneedham.krillionwrapper.game

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lukeneedham.krillionwrapper.R
import com.lukeneedham.krillionwrapper.ui.AppTheme
import com.lukeneedham.krillionwrapper.ui.SettingsButton
import com.lukeneedham.krillionwrapper.ui.SettingsSheet

/**
 * The game's [webView] filling the screen, the retry screen over it when the game failed to load, and (when
 * [showSettings]) the settings gear and its sheet. The view is owned by the view model, not by this composable.
 */
@Composable
internal fun GamePageContent(
    uiState: GameUiState,
    webView: WebView,
    showSettings: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenDebug: () -> Unit,
    onToggleInspector: () -> Unit,
    onShowLayoutReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = AppTheme.current
    val background = colorResource(R.color.background)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            // Keep the game clear of the (visible) system bars and the keyboard.
            .safeDrawingPadding(),
    ) {
        AndroidView(
            // The view outlives this composition, so it may still belong to the previous AndroidView
            factory = {
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
        )
        if (uiState.errorShown) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background)
                    // Swallow touches meant for the WebView underneath.
                    .clickable(interactionSource = null, indication = null) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = stringResource(R.string.error_title),
                    style = TextStyle(color = theme.onBackground, fontFamily = theme.fontFamily, fontSize = 18.sp),
                )
                BasicText(
                    text = stringResource(R.string.error_retry),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .heightIn(min = 48.dp)
                        .background(theme.primary, RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onRetry)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    style = TextStyle(color = theme.onPrimary, fontFamily = theme.fontFamily, fontSize = 16.sp),
                )
            }
        }
        if (showSettings) {
            SettingsButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            )
            SettingsSheet(
                visible = uiState.settingsOpen,
                onDismiss = onCloseSettings,
                onOpenDebug = onOpenDebug,
                inspectorOpen = uiState.inspectorOpen,
                onToggleInspector = onToggleInspector,
                onShowLayoutReport = onShowLayoutReport,
            )
        }
    }
}
