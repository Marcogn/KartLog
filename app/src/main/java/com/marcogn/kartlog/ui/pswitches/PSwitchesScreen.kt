package com.marcogn.kartlog.ui.pswitches

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.PlaceholderScreen

@Composable
fun PSwitchesScreen(onMenuClick: () -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.pswitches_title), onMenuClick = onMenuClick)
}
