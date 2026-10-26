package com.lukeneedham.krillionwrapper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukeneedham.common.ui.CrashesPage
import com.lukeneedham.common.ui.DebugPage
import com.lukeneedham.common.ui.LogsPage

private enum class Screen { Game, Sheet, Debug, Logs, Crashes }

/**
 * Everything the wrapper draws over the game: the settings gear, its bottom sheet and the shared
 * library's debug pages. Where nothing is drawn, touches go through to the game underneath.
 */
@Composable
fun AppOverlay(
    inspectorOpen: Boolean,
    onToggleInspector: () -> Unit,
    onShowLayoutReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var screen by remember { mutableStateOf(Screen.Game) }

    BackHandler(enabled = screen != Screen.Game) {
        screen = when (screen) {
            Screen.Logs, Screen.Crashes -> Screen.Debug
            Screen.Debug -> Screen.Sheet
            else -> Screen.Game
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (screen) {
            Screen.Game, Screen.Sheet -> {
                SettingsButton(
                    onClick = { screen = Screen.Sheet },
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 50.dp, top = 15.dp),
                )
                SettingsSheet(
                    visible = screen == Screen.Sheet,
                    onDismiss = { screen = Screen.Game },
                    onOpenDebug = { screen = Screen.Debug },
                    inspectorOpen = inspectorOpen,
                    onToggleInspector = {
                        screen = Screen.Game
                        onToggleInspector()
                    },
                    onShowLayoutReport = {
                        screen = Screen.Game
                        onShowLayoutReport()
                    },
                )
            }
            else -> Box(
                // The pages are opaque; taps on them must not fall through to the game below.
                Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) {},
            ) {
                when (screen) {
                    Screen.Logs -> LogsPage(onBack = { screen = Screen.Debug })
                    Screen.Crashes -> CrashesPage(onBack = { screen = Screen.Debug })
                    else -> DebugPage(
                        onBack = { screen = Screen.Sheet },
                        onOpenLogs = { screen = Screen.Logs },
                        onOpenCrashes = { screen = Screen.Crashes },
                    )
                }
            }
        }
    }
}
