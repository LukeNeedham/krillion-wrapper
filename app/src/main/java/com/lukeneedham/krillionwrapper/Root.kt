package com.lukeneedham.krillionwrapper

import androidx.compose.runtime.Composable
import com.lukeneedham.common.ui.ProvideDebugSettings
import com.lukeneedham.krillionwrapper.ui.AppOverlay
import com.lukeneedham.krillionwrapper.ui.AppTheme
import com.lukeneedham.krillionwrapper.ui.AppThemeValues
import com.lukeneedham.krillionwrapper.ui.toDebugTheme

/**
 * The one composable [MainActivity] calls. The game itself is a `WebView`, not Compose, so this is only the debug
 * overlay drawn over it: everything it sits in (the [AppTheme] and the debug settings), then the overlay. The app has no
 * pages, so there is no navigation.
 */
@Composable
fun Root(
    inspectorOpen: Boolean,
    onToggleInspector: () -> Unit,
    onShowLayoutReport: () -> Unit,
) {
    val theme = AppThemeValues()
    AppTheme(theme) {
        ProvideDebugSettings(theme = theme.toDebugTheme()) {
            AppOverlay(
                inspectorOpen = inspectorOpen,
                onToggleInspector = onToggleInspector,
                onShowLayoutReport = onShowLayoutReport,
            )
        }
    }
}
