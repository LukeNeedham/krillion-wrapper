package com.lukeneedham.krillionwrapper

import androidx.compose.runtime.Composable
import com.lukeneedham.common.ui.ProvideDebugSettings
import com.lukeneedham.krillionwrapper.ui.AppTheme
import com.lukeneedham.krillionwrapper.ui.AppThemeValues
import com.lukeneedham.krillionwrapper.ui.toDebugTheme

/** The one composable [MainActivity] calls: the theme and the debug settings, then [AppNavigation]. */
@Composable
fun Root() {
    val theme = AppThemeValues()
    AppTheme(theme) {
        ProvideDebugSettings(theme = theme.toDebugTheme()) {
            AppNavigation()
        }
    }
}
