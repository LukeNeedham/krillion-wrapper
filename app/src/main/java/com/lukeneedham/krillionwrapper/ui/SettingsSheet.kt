package com.lukeneedham.krillionwrapper.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lukeneedham.common.debug.scale
import com.lukeneedham.common.ui.LocalAnimationSpeed

/** A bottom sheet over the game with the app's settings and debug tools. */
@Composable
fun SettingsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenDebug: () -> Unit,
    inspectorOpen: Boolean,
    onToggleInspector: () -> Unit,
    onShowLayoutReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = AppTheme.current
    val speed = LocalAnimationSpeed.current
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(speed.scale(200))),
            exit = fadeOut(tween(speed.scale(200))),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(theme.scrim)
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(speed.scale(250))) { it },
            exit = slideOutVertically(tween(speed.scale(200))) { it },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Swallow taps on the sheet itself so they do not reach the scrim and close it.
                    .clickable(interactionSource = null, indication = null) {}
                    .background(theme.surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                SettingsSheetContent(
                    onOpenDebug = onOpenDebug,
                    inspectorOpen = inspectorOpen,
                    onToggleInspector = onToggleInspector,
                    onShowLayoutReport = onShowLayoutReport,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        SettingsSheet(
            visible = true,
            onDismiss = {},
            onOpenDebug = {},
            inspectorOpen = false,
            onToggleInspector = {},
            onShowLayoutReport = {},
        )
    }
}
