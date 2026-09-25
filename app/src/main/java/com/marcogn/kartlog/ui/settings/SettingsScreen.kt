package com.marcogn.kartlog.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.PlaceholderScreen

@Composable
fun SettingsScreen(onMenuClick: () -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.settings_title), onMenuClick = onMenuClick)
}
