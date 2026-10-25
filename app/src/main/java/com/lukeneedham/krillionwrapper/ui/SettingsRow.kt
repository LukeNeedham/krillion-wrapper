package com.lukeneedham.krillionwrapper.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One tappable entry of the settings sheet: a title and a line explaining it. */
@Composable
fun SettingsRow(title: String, detail: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = AppTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        BasicText(
            text = title,
            style = TextStyle(
                color = theme.onSurface,
                fontFamily = theme.fontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        BasicText(
            text = detail,
            style = TextStyle(color = theme.onSurfaceMuted, fontFamily = theme.fontFamily, fontSize = 13.sp),
        )
    }
}
