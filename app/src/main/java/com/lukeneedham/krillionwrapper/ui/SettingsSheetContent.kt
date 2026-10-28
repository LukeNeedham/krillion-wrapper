package com.lukeneedham.krillionwrapper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukeneedham.common.ui.DebugButton
import com.lukeneedham.krillionwrapper.R

/**
 * What the settings sheet shows: the title and the entries. It draws no sheet of its own (no scrim, no rounded
 * surface, no animation), so it can be previewed on its own; [SettingsSheet] puts it in the sheet.
 */
@Composable
fun SettingsSheetContent(
    onOpenDebug: () -> Unit,
    inspectorOpen: Boolean,
    onToggleInspector: () -> Unit,
    onShowLayoutReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = AppTheme.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(
            text = stringResource(R.string.settings_title),
            style = TextStyle(
                color = theme.onSurface,
                fontFamily = theme.fontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        DebugButton(onClick = onOpenDebug, modifier = Modifier.fillMaxWidth())
        SettingsRow(
            title = stringResource(
                if (inspectorOpen) R.string.settings_inspect_hide_title else R.string.settings_inspect_show_title,
            ),
            detail = stringResource(R.string.settings_inspect_detail),
            onClick = onToggleInspector,
        )
        SettingsRow(
            title = stringResource(R.string.settings_layout_title),
            detail = stringResource(R.string.settings_layout_detail),
            onClick = onShowLayoutReport,
        )
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        SettingsSheetContent(
            onOpenDebug = {},
            inspectorOpen = false,
            onToggleInspector = {},
            onShowLayoutReport = {},
            modifier = Modifier.background(AppTheme.current.surface).padding(20.dp),
        )
    }
}

@Preview
@Composable
private fun PreviewInspectorOpen() {
    AppTheme {
        SettingsSheetContent(
            onOpenDebug = {},
            inspectorOpen = true,
            onToggleInspector = {},
            onShowLayoutReport = {},
            modifier = Modifier.background(AppTheme.current.surface).padding(20.dp),
        )
    }
}
