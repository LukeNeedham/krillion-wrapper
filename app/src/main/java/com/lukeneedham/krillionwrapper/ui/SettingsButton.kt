package com.lukeneedham.krillionwrapper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.krillionwrapper.R

/**
 * The small rectangular gear that floats over the game and opens the settings sheet, styled like the
 * game's own menu buttons: a translucent dark fill, a thin darker border and a muted icon.
 */
@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = AppTheme.current
    val description = stringResource(R.string.settings_open)
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 30.dp)
            .background(theme.background.copy(alpha = 0.4f))
            .border(2.dp, theme.background.copy(alpha = 0.8f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(R.string.settings_icon),
            style = TextStyle(color = theme.onSurfaceMuted, fontSize = 16.sp),
        )
    }
}
